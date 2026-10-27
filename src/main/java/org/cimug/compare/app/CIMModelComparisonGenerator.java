package org.cimug.compare.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.cimug.compare.PackageNames;
import org.cimug.compare.report.ReportRenderer;
import org.sparx.Collection;
import org.sparx.EnumXMIType;
import org.sparx.Package;
import org.sparx.Project;
import org.sparx.Repository;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class CIMModelComparisonGenerator {

	private static final String PARAM_PACKAGE = "package";
	/** The packages compared when several --package names were exported from EA projects (#94). */
	private static List<String> selectedPackages = null;
	private static final String PARAM_MINIMAL = "minimal"; // 1.x option; accepted and ignored (minimal is now the default)
	private static final String PARAM_FULL = "full";
	private static final String PARAM_INCLUDE_DIAGRAMS = "include-diagrams";
	private static final String PARAM_ZIP = "zip";
	/** The release, as in the jar name cim-compare-<VERSION>.jar (from the pom, #87). */
	private static final String VERSION = org.cimug.compare.Version.VERSION;

	private static final String PARAM_CLEANUP = "cleanup";

	/**
	 * Files and folders this run created before the report (the comparison XML when
	 * it was generated, and the XMI files and image folders exported from EA
	 * projects). With --zip, --cleanup deletes these plus the report and the
	 * enriched XML, and nothing else: files the user supplied are never deleted
	 * (issue #53).
	 */
	private static final List<File> createdByRun = new LinkedList<File>();
	private static final String PARAM_IMAGE_TYPE = "image-type";
	private static final String ANSI = "windows-1252";
	private static final String UTF8 = "UTF-8";

	private static final String XML = ".xml";
	private static final String XMI = ".xmi";
	private static final String HTM = ".htm";
	private static final String HTML = ".html";
	private static final Set<String> EA_PROJECT_EXT = new HashSet<String>(Arrays.asList(".eap", ".eapx", "qea", ".qeax", ".feap"));
	private static final Set<String> HTML_EXT = new HashSet<String>(Arrays.asList(".htm", ".html"));
	private static final String ZIP = ".zip";

	static enum DiagramXML {
		NO_EXPORT(0), EXPORT_WITHOUT_IMAGES(1), EXPORT_WITH_IMAGES(2);

		private final int code;

		DiagramXML(int code) {
			this.code = code;
		}

		public int code() {
			return code;
		}
	}

	static enum DiagramImage {
		NONE(-1, null), EMF(0, "emf"), BMP(1, "bmp"), GIF(2, "gif"), PNG(3, "png"), JPG(4, "jpg");

		private final int code;
		private final String ext;

		DiagramImage(int code, String ext) {
			this.code = code;
			this.ext = ext;
		}

		public int code() {
			return code;
		}
		
		public String ext() {
			return ext;
		}
	}

	/**
	 * Removes Java's default XML processing limits, which the models and
	 * comparison files cim-compare reads (the user's own files) easily exceed.
	 * Setting a limit to 0 removes it. Must run before any XML parser, XPath or
	 * XSLT processor is created.
	 * <ul>
	 * <li>XPath limits, introduced in Java 11 (#21).</li>
	 * <li>Entity size limits, lowered in Java 24 (JDK-8343006) to 100,000:
	 * reading a large comparison XML failed with JAXP00010003 / JAXP00010004
	 * (#65).</li>
	 * </ul>
	 * Native access for EA's Java API (JEP 472, Java 24) is enabled by the
	 * {@code Enable-Native-Access: ALL-UNNAMED} attribute in the jar manifest
	 * (pom.xml), as it cannot be set from code.
	 */
	public static void removeXmlProcessingLimits() {
		System.setProperty("jdk.xml.xpathExprGrpLimit", "0");
		System.setProperty("jdk.xml.xpathExprOpLimit", "0");
		System.setProperty("jdk.xml.xpathTotalOpLimit", "0");
		System.setProperty("jdk.xml.totalEntitySizeLimit", "0");
		System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", "0");
	}

	public static void main(String[] args) {
		selectedPackages = null;

		removeXmlProcessingLimits();

		
		Map<String, String> options = parseCommandLineOptions(args);

		File[] fileArgs = parseFileArguments(args, options);

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

			File compareLogXMLFile = fileArgs[1];
			File comparisonHTMLFile = fileArgs[2];

			DocumentBuilder builder = null;
			Document document = null;
			InputSource is = null;

			try {
				builder = factory.newDocumentBuilder();
				is = new InputSource(new FileInputStream(compareLogXMLFile));
				is.setEncoding(UTF8);
				document = builder.parse(is);
			} catch (Exception e) {
				try {
					builder = factory.newDocumentBuilder();
					document = builder.parse(compareLogXMLFile);
				} catch (Exception e2) {
					builder = factory.newDocumentBuilder();
					is = new InputSource(new FileInputStream(compareLogXMLFile));
					is.setEncoding(ANSI);
					document = builder.parse(is);
				}
			}

			/**
			 * Stage 3: comparison XML -> enriched XML (written beside the report for
			 * debugging and for other tooling) -> HTML. See ReportRenderer.
			 */
			ReportRenderer renderer = selectedPackages != null
					? ReportRenderer.forPackages(options.containsKey(PARAM_FULL), selectedPackages,
							options.containsKey(PARAM_INCLUDE_DIAGRAMS), options.get(PARAM_IMAGE_TYPE))
					: new ReportRenderer(options.containsKey(PARAM_FULL), options.get(PARAM_PACKAGE),
							options.containsKey(PARAM_INCLUDE_DIAGRAMS), options.get(PARAM_IMAGE_TYPE));
			File enrichedXMLFile = renderer.render(document, compareLogXMLFile, comparisonHTMLFile);
			document = null;

			if (comparisonHTMLFile != null) {
				System.out.println("\nCIM model comparison report successfully generated:  \n" + comparisonHTMLFile.getAbsolutePath());
				System.out.println("\nEnriched comparison XML (report input, for debugging):  \n" + enrichedXMLFile.getAbsolutePath());
			}

			File zipFile = createZipArchive(fileArgs, options);

			if (zipFile != null) {
				System.out.println(
						"\nCIM model comparison report ZIP archive generated:  \n" + zipFile.getAbsolutePath());
			}
		} catch (IllegalArgumentException e) {
			// A problem with the command line or the input (e.g. --package names a package
			// that is not in the comparison): report it plainly, without a stack trace.
			System.err.println("ERROR:  " + e.getMessage());
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @param option
	 *            The command line option that was invalid.
	 */
	private static void exitOnInvalidParameter(String option) {
		System.err.print("Invalid or missing command line option: " + (option.startsWith("-") ? "" : "--") + option);
		System.err.println();
		printUsage();
		System.exit(1);
	}

	private static Map<String, String> parseCommandLineOptions(String[] args) {

		if (((args.length == 1) && //
				("--help".equals(args[0].toLowerCase()) || //
						"-help".equals(args[0].toLowerCase()) || //
						"--h".equals(args[0].toLowerCase()) || //
						"-h".equals(args[0].toLowerCase())))
				|| //
				(args.length < 1 || ((args.length > 3)
						&& (!args[args.length - 1].startsWith("--") && !args[args.length - 1].startsWith("-"))))) {
			printUsage();
			System.exit(1);
		}

		Map<String, String> options = new HashMap<String, String>();

		for (String arg : args) {
			if (arg.startsWith("--") || arg.startsWith("-")) {
				String param = (arg.startsWith("--") ? arg.replaceFirst("--", "") : arg.replaceFirst("-", ""));
				String value = null;

				if (arg.contains("=")) {
					param = param.substring(0, param.indexOf("="));
					value = arg.substring(arg.indexOf("=") + 1);
				}

				switch (param.toLowerCase())
					{
					case PARAM_MINIMAL: // accepted for compatibility with 1.x; minimal output is the default
					case PARAM_FULL:
					case PARAM_INCLUDE_DIAGRAMS:
					case PARAM_ZIP:
					case PARAM_CLEANUP:
						value = Boolean.TRUE.toString(); // flags carry no value...
						break;
					case PARAM_IMAGE_TYPE:
						if ((value != null) && (!"".equals(value))) {
							try {
								DiagramImage.valueOf(value.toUpperCase());
								// extensions on images must be lower case...
								value = value.toLowerCase(); 
							} catch (Exception e) {
								String validValues = "";

								DiagramImage[] diagramImageTypes = DiagramImage.values();

								for (int index = 0; index <= diagramImageTypes.length; index++) {
									validValues += (diagramImageTypes[index] != DiagramImage.NONE
											? diagramImageTypes[index].name()
													+ (index <= diagramImageTypes.length - 2 ? ", " : "")
											: "");
								}

								System.err.print(PARAM_IMAGE_TYPE + " must be one of: " + validValues);

								exitOnInvalidParameter(arg);
							}
						} else {
							exitOnInvalidParameter(arg);
						}
						break;
					case PARAM_PACKAGE:
						if ((value == null) || PackageNames.parse(value).isEmpty()) {
							exitOnInvalidParameter(arg);
						}
						break;
					default:
						exitOnInvalidParameter(arg);
						break;
					}

				options.put(param, value);
			}
		}

		if (!options.containsKey(PARAM_IMAGE_TYPE)) {
			/**
			 * When processing .EAP/.QEA files and exporting diagrams we ensure that a default
			 * image type of 'JPG' is used if no image-type is explicitly specified on the
			 * command line.
			 */
			options.put(PARAM_IMAGE_TYPE, DiagramImage.JPG.ext());
		}

		return options;
	}
	
	private static boolean isProjectFile(File file) {
		for (String ext : EA_PROJECT_EXT) {
			if (file.getName().toLowerCase().endsWith(ext)) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean isHTMLFile(File file) {
		for (String ext : HTML_EXT) {
			if (file.getName().toLowerCase().endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	private static File[] parseFileArguments(String[] args, Map<String, String> options) {

		File modelComparisonXMLFile = null;
		File targetOutputHTMLFile = null;
		createdByRun.clear();

		List<File> fileArgs = new LinkedList<File>();

		for (String arg : args) {
			if (!arg.startsWith("--") && !arg.startsWith("-")) {
				fileArgs.add(new File(arg));
			}
		}

		File[] arguments = fileArgs.toArray(new File[fileArgs.size()]);

		boolean isValid = true;

		for (File arg : arguments) {
			if ((arg.getName().toLowerCase().indexOf(".") == -1) && !arg.exists()) {
				if (!arg.mkdirs()) {
					System.err.println("ERROR:  Unable to create output directory:  " + arg.getAbsolutePath());
					System.exit(1);
				}
			}
			// Note that the ternary condition is to ensure that we only test that a file or
			// directory exists IF it is an argument that does not end in .html or .htm.
			// This is because when an explicit HTML file is specified (as opposed to a
			// target output directory) the file is not assumed to exist yet...
			isValid = isValid
					&& (!isHTMLFile(arg)
							? arg.exists()
							: true);
		}

		if (!isValid) {
			System.err.println(
					"ERROR:  One or more of the files or directories passed as arguments either do not exist or cannot be located on the file system. Verify the file or directory names specified or provide the absolute paths if necessary.");
			System.err.println();
			System.err.println("Invalid command line argument(s): ");
			for (File argument : arguments) {
				if (!(argument.getName().toLowerCase().contains(".") && argument.getName().toLowerCase().endsWith(HTML)
						|| argument.getName().toLowerCase().endsWith(HTM)) && !argument.exists()) {
					// MUST be specified using the getPath() method. This method
					// will properly reflect what was passed on the command line.
					System.err.println("   " + argument.getPath());
				}
			}
			System.exit(1);
		}

		if ((arguments.length == 1
				&& (arguments[0].isDirectory() || !arguments[0].getName().toLowerCase().endsWith(XML))) || //
				(arguments.length >= 1 && arguments[0].isDirectory()) || //
				(arguments.length == 2 && ((arguments[0].getName().toLowerCase().endsWith(XMI)
						&& !arguments[1].getName().toLowerCase().endsWith(XMI))
						|| (arguments[0].getName().toLowerCase().endsWith(XML)
								&& (!(arguments[1].getName().toLowerCase().endsWith(HTML)
										|| arguments[1].getName().toLowerCase().endsWith(HTM)
										|| arguments[1].isDirectory())))))
				|| //
				(arguments.length > 2 && ((!((arguments[0].getName().toLowerCase().endsWith(XMI)
						&& arguments[1].getName().toLowerCase().endsWith(XMI))
						|| (isProjectFile(arguments[0]) && isProjectFile(arguments[1])))) //
						|| (!arguments[2].isDirectory() && !isHTMLFile(arguments[2]))))) {
			System.err.print("ERROR:  Invalid command line usage. ");
			if (arguments.length == 1) {
				System.err.print(
						"When passing in a single command line argument it must be a valid Enterprise Architect comparison results (XML) file.");
			}
			System.err.println();
			printUsage();
			System.exit(1);
		}

		File outputDir = null;
		String baselineXmiFile = null;
		String destinationXmiFile = null;

		// At this point we've vetted out the validity of the arguments and that they
		// exist on the file system.
		if (!arguments[0].getName().toLowerCase().endsWith(XML)) {

			int baselineFileExtIndex = arguments[0].getName().toLowerCase().lastIndexOf(".");
			int destinationFileExtIndex = arguments[1].getName().toLowerCase().lastIndexOf(".");
			//
			String baselineFileExt = arguments[0].getName().toLowerCase().substring(baselineFileExtIndex);
			String destinationFileExt = arguments[1].getName().toLowerCase().substring(destinationFileExtIndex);

			String defaultComparisonXMLFileName = "CIMModelComparison_"
					+ arguments[0].getName().replace(baselineFileExt, "") + "_AND_"
					+ arguments[1].getName().replace(destinationFileExt, "") + XML;

			String defaultComparisonHTMLFileName = "CIMModelComparison_"
					+ arguments[0].getName().replace(baselineFileExt, "") + "_AND_"
					+ arguments[1].getName().replace(destinationFileExt, "") + HTML;

			// Determine the output directory...
			switch (arguments.length)
				{
				case 2:
					// Neither a target output directory or target HTML file was specified...
					outputDir = (arguments[0].getParentFile() != null ? arguments[0].getParentFile() : new File("."));
					break;
				case 3:
					if (arguments[2].isDirectory()) {
						// A target output directory or target HTML file was specified...
						outputDir = arguments[2];
					} else {
						// We know now that argument two is the name of an HTML report file
						// so we use that name to derive the XML file name...
						outputDir = (arguments[2].getParentFile() != null ? arguments[2].getParentFile()
								: (arguments[0].getParentFile() != null ? arguments[0].getParentFile()
										: new File(".")));

						defaultComparisonXMLFileName = arguments[2].getName().replace(HTML, "") + XML;
						defaultComparisonHTMLFileName = arguments[2].getName();
					}
					break;
				}

			/**
			 * Finally, we want to ensure that the output directory exists and if not we
			 * create it. This may occur due to a couple of reasons, for example:
			 * 
			 * When the HTML report file is specified instead of an output directory then
			 * the output directory will be the directory of the HTML comparison report. In
			 * turn, the specified HTML file may or may not exist yet. If it exists it will
			 * be overridden. When it does not exist we must ensure that the directory it
			 * will be contained in also exists and if not we create it.
			 */
			if (!outputDir.exists()) {
				if (!outputDir.mkdirs()) {
					System.err.println("ERROR:  Unable to create output directory:  " + outputDir.getAbsolutePath());
					System.exit(1);
				}
			}

			modelComparisonXMLFile = new File(outputDir, defaultComparisonXMLFileName);
			targetOutputHTMLFile = new File(outputDir, defaultComparisonHTMLFileName);
			// Generated from the two models by this run (not an input).
			createdByRun.add(modelComparisonXMLFile);

			System.out.println("\nOutput directory confirmed:  \n" + outputDir.getAbsolutePath());

			/**
			 * We've determined above the modelComparisonXMLFile & targetOutputHTMLFiles and
			 * now transition to testing if the input files are EA Project files which require the
			 * additional step of first exporting the baseline and target XMI files before
			 * executing a call to the DiffXMLGenerator.
			 */
			if (isProjectFile(arguments[0])) {

				DiagramXML diagramXML = (options.containsKey(PARAM_INCLUDE_DIAGRAMS) ? DiagramXML.EXPORT_WITHOUT_IMAGES
						: DiagramXML.NO_EXPORT);

				DiagramImage diagramImage = (options.containsKey(PARAM_IMAGE_TYPE)
						? DiagramImage.valueOf(options.get(PARAM_IMAGE_TYPE).toUpperCase())
						: DiagramImage.NONE);

				String thePackageName = (options.containsKey(PARAM_PACKAGE) ? options.get(PARAM_PACKAGE) : null);

				/**
				 * Export the baseline (argument 0) and destination (argument 1) projects.
				 *
				 * When --package is given, the package is located by name in each project
				 * (root nodes included). A package renamed between the two versions (e.g.
				 * IEC61970 -> Grid) is found by name in only one of them; its counterpart in
				 * the other is then located by GUID. If the package cannot be resolved on
				 * both sides processing stops, rather than silently comparing the whole
				 * model (see issue #49).
				 */
				File baselineXmi = new File(outputDir, baseName(arguments[0]) + XMI);
				File destinationXmi = new File(outputDir, baseName(arguments[1]) + XMI);

				// Several --package names (#94); a single name keeps the path below.
				List<String> packageList = thePackageName == null ? null : PackageNames.parse(thePackageName);
				boolean listExported = false;
				if (packageList != null && packageList.size() > 1) {
					List<String> compared = exportPackageList(arguments, outputDir, baselineXmi, destinationXmi,
							thePackageName.trim(), packageList, diagramXML, diagramImage);
					if (compared != null) {
						selectedPackages = compared;
						listExported = true;
					} else {
						thePackageName = thePackageName.trim(); // the name of one package, containing a comma
					}
				} else if (packageList != null) {
					thePackageName = packageList.get(0);
				}

				String baselineGuid = listExported ? null
						: exportProject(arguments[0], true, outputDir, baselineXmi, thePackageName, null, diagramXML,
								diagramImage);
				String destinationGuid = listExported ? null
						: exportProject(arguments[1], false, outputDir, destinationXmi, thePackageName, baselineGuid,
								diagramXML, diagramImage);

				if (thePackageName != null && !listExported) {
					if (baselineGuid == null && destinationGuid == null) {
						System.err.println("ERROR:  Package '" + thePackageName
								+ "' was not found in either the baseline or the destination model.");
						System.exit(1);
					}
					if (destinationGuid == null) {
						System.err.println("ERROR:  Package '" + thePackageName + "' was found in the baseline model (GUID "
								+ baselineGuid
								+ ") but the destination model has no package with that name or GUID.");
						System.exit(1);
					}
					if (baselineGuid == null) {
						// Found in the destination only: export the baseline's package with the same GUID.
						baselineGuid = exportProject(arguments[0], true, outputDir, baselineXmi, thePackageName,
								destinationGuid, diagramXML, diagramImage);
						if (baselineGuid == null) {
							System.err.println("ERROR:  Package '" + thePackageName
									+ "' was found in the destination model (GUID " + destinationGuid
									+ ") but the baseline model has no package with that name or GUID.");
							System.exit(1);
						}
					} else if (!baselineGuid.equals(destinationGuid)) {
						System.out.println("\nWARNING:  The packages named '" + thePackageName
								+ "' in the baseline and destination models have different GUIDs (" + baselineGuid + ", "
								+ destinationGuid + "). They are compared as different packages.");
					}
				}

				baselineXmiFile = baselineXmi.getAbsolutePath();
				destinationXmiFile = destinationXmi.getAbsolutePath();

				// Exported from the EA projects by this run. (With XMI files as input the
				// XMI files and image folders are the user's own exports.)
				createdByRun.add(baselineXmi);
				createdByRun.add(destinationXmi);
				if (options.containsKey(PARAM_INCLUDE_DIAGRAMS)) {
					createdByRun.add(new File(outputDir, "Images-baseline"));
					createdByRun.add(new File(outputDir, "Images-destination"));
				}
			} else {
				// We have determined that the two input files are XMI files so we simply
				// set the baselineXMIInputFiles & targetXMIInputFiles variables to the
				// values of arguments[0] and arguments[1] respectively.
				baselineXmiFile = arguments[0].getAbsolutePath();
				destinationXmiFile = arguments[1].getAbsolutePath();
			}

			DiffXMLGenerator.main(
					new String[] { baselineXmiFile, destinationXmiFile, modelComparisonXMLFile.getAbsolutePath(), (options.containsKey(PARAM_IMAGE_TYPE) ? options.get(PARAM_IMAGE_TYPE) : DiagramImage.JPG.name())});

			System.out.println("\nCompare Log XML report generated:  \n" + modelComparisonXMLFile.getAbsolutePath());
		} else {

			// Finally, we cover the case where we have a single XML EA comparison file to
			// be processed into an HTML comparison report output file...

			modelComparisonXMLFile = arguments[0];

			// Default name is based on the same name as the XML comparison file name...
			String defaultOutputHTMLFileName = arguments[0].getName().replace(XML, HTML);

			/**
			 * Determine the output directory...
			 */
			switch (arguments.length)
				{
				case 1:
					// Neither a target output directory or target HTML file was specified.
					// We specify that the file will reside in the same parent directory
					// that the XML file resides in...
					outputDir = (arguments[0].getParentFile() != null ? arguments[0].getParentFile() : new File("."));
					break;
				case 2:
					// A target output directory or target HTML file was specified...
					if (arguments[1].isDirectory()) {
						// If a directory was specified we use it as the parent directory and then
						// use the default output HTML file name...
						outputDir = arguments[1];
					} else {
						outputDir = (arguments[1].getParentFile() != null ? arguments[1].getParentFile()
								: (arguments[0].getParentFile() != null ? arguments[0].getParentFile()
										: new File(".")));
						defaultOutputHTMLFileName = arguments[1].getName();
					}
					break;
				}

			/**
			 * Again, we want to ensure that the output directory exists and if not we
			 * create it.
			 */
			if (!outputDir.exists()) {
				if (!outputDir.mkdirs()) {
					System.err.println("ERROR:  Unable to create output directory:  " + outputDir.getAbsolutePath());
					System.exit(1);
				}
			}

			System.out.println("\nOutput directory confirmed:  \n" + outputDir.getAbsolutePath());

			targetOutputHTMLFile = new File(outputDir, defaultOutputHTMLFileName);
		}

		File[] results;

		if (baselineXmiFile == null && destinationXmiFile == null) {
			results = new File[] { outputDir, modelComparisonXMLFile, targetOutputHTMLFile };
		} else {
			results = new File[] { outputDir, modelComparisonXMLFile, targetOutputHTMLFile, new File(baselineXmiFile),
					new File(destinationXmiFile) };
		}

		return results;
	}

	private static String baseName(File file) {
		String name = file.getName();
		int dot = name.lastIndexOf(".");
		return dot > 0 ? name.substring(0, dot) : name;
	}

	/**
	 * Opens an EA project and exports one package of it to XMI 1.1 (plus diagram
	 * images when requested).
	 *
	 * <ul>
	 * <li>No package name: the project's root node is exported. When the project has
	 * more than one root node, each is exported and the exports are merged into one
	 * XMI file (see {@link #exportAllRoots}, issue #88).</li>
	 * <li>A package name: the package of that name is exported, searching root nodes
	 * and everything below them. If there is none and {@code guidHint} is given, the
	 * package with that GUID is exported instead (a package renamed between versions).
	 * If neither is found nothing is exported and null is returned.</li>
	 * </ul>
	 *
	 * @return the GUID of the exported package (the first root node when several
	 *         are merged), or null when nothing was exported
	 */
	private static String exportProject(File eaProject, boolean baseline, File outputDir, File xmiExportFile,
			String packageName, String guidHint, DiagramXML diagramXML, DiagramImage diagramImage) {
		String side = baseline ? "Baseline" : "Destination";
		Repository repository = null;
		boolean failed = false;
		try {
			repository = new Repository();
			repository.OpenFile(eaProject.getAbsolutePath());

			Collection<Package> models = repository.GetModels();
			Package packageToCompare = null;

			if (packageName == null) {
				if (models.GetCount() > 1)
					return exportAllRoots(repository, models, eaProject, side, baseline, outputDir, xmiExportFile,
							diagramXML, diagramImage);
				packageToCompare = models.GetAt((short) 0);
			} else {
				for (Package m : models) {
					packageToCompare = m.GetName().equals(packageName) ? m : findPackage(packageName, m.GetPackages());
					if (packageToCompare != null)
						break;
				}
				if (packageToCompare == null && guidHint != null) {
					try {
						packageToCompare = repository.GetPackageByGuid(guidHint);
					} catch (Exception notFound) {
						packageToCompare = null;
					}
					if (packageToCompare != null) {
						System.out.println("\n" + side + " model: package '" + packageName + "' not found by name; matched by GUID "
								+ guidHint + " as '" + packageToCompare.GetName() + "'.");
					}
				}
				if (packageToCompare == null) {
					if (guidHint == null && baseline) {
						System.out.println("\n" + side + " model: package '" + packageName
								+ "' not found by name; will look it up by GUID after the destination model is read.");
					}
					return null;
				}
			}

			repository.GetProjectInterface().ExportPackageXMI(packageToCompare.GetPackageGUID(), EnumXMIType.xmiEA11, diagramXML.code(),
					diagramImage.code(), 1, 0, xmiExportFile.getAbsolutePath());

			if (diagramXML != DiagramXML.NO_EXPORT) {
				File imagesDirectory = new File(outputDir, "Images");
				if (imagesDirectory.exists()) {
					File newImagesDirectory = new File(outputDir, "Images" + (baseline ? "-baseline" : "-destination"));
					// Not a plain rename: on Windows it can fail while EA has the project open,
					// leaving the images for the next export to overwrite (issue #91).
					moveImages(imagesDirectory, newImagesDirectory);
					System.out.println("\n" + side + " model diagrams successfully exported as " + diagramImage.name()
							+ " images to:  \n" + newImagesDirectory.getAbsolutePath());
				} else {
					System.err.println("ERROR:  Unable to export diagram images. Terminating EA .eap XMI export processing.");
					System.exit(1);
				}
			}

			System.out.println("\n" + side + " model XMI export completed successfully (package '"
					+ packageToCompare.GetName() + "'):  \n" + xmiExportFile.getAbsolutePath());
			return packageToCompare.GetPackageGUID();
		} catch (Exception e) {
			failed = true;
			e.printStackTrace();
			return null;
		} finally {
			// We must explicitly make a GC call. This is due to a
			// limitation in Sparx EA's Java API and memory...
			System.gc();
			if (repository != null) {
				// Required by EA's automation API to ensure everything terminates properly.
				repository.CloseFile();
				repository.Exit();
				repository = null;
			}
			if (failed) {
				System.err.println("ERROR:  Terminating XMI export processing for EA project file [" + eaProject.getName()
						+ "] due to an unexpected exception.");
				System.err.println();
				System.exit(1);
			}
		}
	}

	/** The packages of one project that several --package names resolve to (#94). */
	private static final class Selection {
		/** The whole --package value is the name of one package: use the single-name path. */
		boolean wholeName;
		/** All names were found and the packages were exported. */
		boolean exported;
		/** Names kept after dropping packages nested in another listed package. */
		List<String> names = new LinkedList<String>();
		/** GUID of each name's package in this project, or null when not found. */
		Map<String, String> guids = new java.util.LinkedHashMap<String, String>();
	}

	/**
	 * Exports the packages named by several --package names from both projects
	 * (issue #94). Each name is resolved as a single --package is: by name in each
	 * project, and by GUID in the other project when the package was renamed
	 * (#49). A name not found in both projects stops the run. A package inside
	 * another listed package is dropped with a warning.
	 *
	 * @return the names compared, in the order given, or null when the whole value
	 *         is the name of a single package (a name containing a comma)
	 */
	private static List<String> exportPackageList(File[] arguments, File outputDir, File baselineXmi,
			File destinationXmi, String whole, List<String> names, DiagramXML diagramXML, DiagramImage diagramImage) {
		Selection b = selectPackages(arguments[0], true, outputDir, baselineXmi, whole, names, null, diagramXML,
				diagramImage);
		if (b.wholeName)
			return null;
		Selection d = selectPackages(arguments[1], false, outputDir, destinationXmi, b.exported ? null : whole,
				b.exported ? b.names : names, b.guids, diagramXML, diagramImage);
		if (d.wholeName)
			return null;
		if (!d.exported) {
			for (Map.Entry<String, String> e : d.guids.entrySet()) {
				if (e.getValue() != null)
					continue;
				String bg = b.guids.get(e.getKey());
				if (bg == null)
					System.err.println("ERROR:  Package '" + e.getKey()
							+ "' was not found in either the baseline or the destination model.");
				else
					System.err.println("ERROR:  Package '" + e.getKey() + "' was found in the baseline model (GUID " + bg
							+ ") but the destination model has no package with that name or GUID.");
			}
			System.exit(1);
		}
		if (!b.exported) {
			// Some names were found in the destination only: export the baseline's
			// packages, using the destination's GUIDs for those.
			Selection b2 = selectPackages(arguments[0], true, outputDir, baselineXmi, null, d.names, d.guids,
					diagramXML, diagramImage);
			if (!b2.exported) {
				for (Map.Entry<String, String> e : b2.guids.entrySet())
					if (e.getValue() == null)
						System.err.println("ERROR:  Package '" + e.getKey() + "' was found in the destination model (GUID "
								+ d.guids.get(e.getKey())
								+ ") but the baseline model has no package with that name or GUID.");
				System.exit(1);
			}
			b = b2;
		}
		for (String name : d.names) {
			String bg = b.guids.get(name), dg = d.guids.get(name);
			if (bg != null && dg != null && !bg.equals(dg))
				System.out.println("\nWARNING:  The packages named '" + name
						+ "' in the baseline and destination models have different GUIDs (" + bg + ", " + dg
						+ "). They are compared as different packages.");
		}
		return d.names;
	}

	/**
	 * Opens an EA project and finds the package for each name: by name (root nodes
	 * included), else by the GUID {@code hints} gives for that name. When every
	 * name is found, packages inside another listed package are dropped and the
	 * rest are exported to {@code xmiExportFile} (merged when there are several).
	 */
	private static Selection selectPackages(File eaProject, boolean baseline, File outputDir, File xmiExportFile,
			String whole, List<String> names, Map<String, String> hints, DiagramXML diagramXML,
			DiagramImage diagramImage) {
		String side = baseline ? "Baseline" : "Destination";
		Selection sel = new Selection();
		Repository repository = null;
		boolean failed = false;
		try {
			repository = new Repository();
			repository.OpenFile(eaProject.getAbsolutePath());
			Collection<Package> models = repository.GetModels();

			if (whole != null && findByName(models, whole) != null) {
				sel.wholeName = true;
				return sel;
			}

			Map<String, Package> found = new java.util.LinkedHashMap<String, Package>();
			for (String name : names) {
				Package p = findByName(models, name);
				String hint = hints == null ? null : hints.get(name);
				if (p == null && hint != null) {
					try {
						p = repository.GetPackageByGuid(hint);
					} catch (Exception notFound) {
						p = null;
					}
					if (p != null)
						System.out.println("\n" + side + " model: package '" + name + "' not found by name; matched by GUID "
								+ hint + " as '" + p.GetName() + "'.");
				}
				if (p == null && hints == null && baseline)
					System.out.println("\n" + side + " model: package '" + name
							+ "' not found by name; will look it up by GUID after the destination model is read.");
				sel.guids.put(name, p == null ? null : p.GetPackageGUID());
				if (p != null)
					found.put(name, p);
			}
			if (found.size() < names.size())
				return sel;

			// A package inside another listed package is reported as part of it.
			Set<String> listed = new HashSet<String>(sel.guids.values());
			List<Package> packages = new LinkedList<Package>();
			for (String name : names) {
				Package p = found.get(name);
				Package outer = listedAncestor(repository, p, listed);
				if (outer != null) {
					System.out.println("\nWARNING:  Package '" + name + "' is inside '" + outer.GetName()
							+ "', which is also given with --package; it is reported as part of '" + outer.GetName()
							+ "'.");
					continue;
				}
				sel.names.add(name);
				packages.add(p);
			}

			if (packages.size() == 1) {
				repository.GetProjectInterface().ExportPackageXMI(packages.get(0).GetPackageGUID(),
						EnumXMIType.xmiEA11, diagramXML.code(), diagramImage.code(), 1, 0,
						xmiExportFile.getAbsolutePath());
				File imagesDirectory = new File(outputDir, "Images");
				File sideImages = new File(outputDir, "Images" + (baseline ? "-baseline" : "-destination"));
				if (diagramXML != DiagramXML.NO_EXPORT) {
					if (!imagesDirectory.exists()) {
						System.err.println("ERROR:  Unable to export diagram images. Terminating EA .eap XMI export processing.");
						System.exit(1);
					}
					moveImages(imagesDirectory, sideImages);
					System.out.println("\n" + side + " model diagrams successfully exported as " + diagramImage.name()
							+ " images to:  \n" + sideImages.getAbsolutePath());
				}
				System.out.println("\n" + side + " model XMI export completed successfully (package '"
						+ packages.get(0).GetName() + "'):  \n" + xmiExportFile.getAbsolutePath());
			} else {
				exportMerged(repository, packages, "package", eaProject, side, baseline, outputDir, xmiExportFile,
						diagramXML, diagramImage);
				System.out.println("\n" + side + " model XMI export completed successfully (packages "
						+ PackageNames.join(sel.names) + ", merged):  \n" + xmiExportFile.getAbsolutePath());
			}
			sel.exported = true;
			return sel;
		} catch (Exception e) {
			failed = true;
			e.printStackTrace();
			return sel;
		} finally {
			System.gc();
			if (repository != null) {
				repository.CloseFile();
				repository.Exit();
				repository = null;
			}
			if (failed) {
				System.err.println("ERROR:  Terminating XMI export processing for EA project file [" + eaProject.getName()
						+ "] due to an unexpected exception.");
				System.err.println();
				System.exit(1);
			}
		}
	}

	/** The package's nearest ancestor whose GUID is in {@code listed}, or null. */
	private static Package listedAncestor(Repository repository, Package p, Set<String> listed) {
		int parent = p.GetParentID();
		while (parent != 0) {
			Package a;
			try {
				a = repository.GetPackageByID(parent);
			} catch (Exception notFound) {
				return null;
			}
			if (a == null)
				return null;
			if (listed.contains(a.GetPackageGUID()))
				return a;
			parent = a.GetParentID();
		}
		return null;
	}

	/** The package of that name among the root nodes or anywhere below them, or null. */
	private static Package findByName(Collection<Package> models, String name) {
		for (Package m : models) {
			Package p = m.GetName().equals(name) ? m : findPackage(name, m.GetPackages());
			if (p != null)
				return p;
		}
		return null;
	}

	/**
	 * Exports every root node of an open EA project and merges the exports into
	 * {@code xmiExportFile} (issue #88). EA exports one package per XMI file, so
	 * each root goes to its own file first; {@link XmiMerger} then puts them under
	 * one model, where references from one root to another resolve like any other.
	 * The per-root files are deleted after the merge. Diagram images from all roots
	 * go to the side's images folder.
	 *
	 * @return the GUID of the first root node
	 */
	private static String exportAllRoots(Repository repository, Collection<Package> models, File eaProject,
			String side, boolean baseline, File outputDir, File xmiExportFile, DiagramXML diagramXML,
			DiagramImage diagramImage) throws Exception {
		List<Package> roots = new LinkedList<Package>();
		StringBuilder names = new StringBuilder();
		for (Package m : models) {
			roots.add(m);
			names.append(names.length() > 0 ? ", " : "").append(m.GetName());
		}
		System.out.println("\n" + side + " project " + eaProject.getName() + " has " + roots.size()
				+ " root nodes (" + names + "). All are compared.");

		exportMerged(repository, roots, "root node", eaProject, side, baseline, outputDir, xmiExportFile,
				diagramXML, diagramImage);
		System.out.println("\n" + side + " model XMI export completed successfully (root nodes " + names
				+ ", merged):  \n" + xmiExportFile.getAbsolutePath());
		return roots.get(0).GetPackageGUID();
	}

	/**
	 * Exports each of {@code packages} of an open EA project to its own XMI file
	 * and merges them into {@code xmiExportFile} with {@link XmiMerger} (issues #88,
	 * #94). The per-package files are deleted after the merge; diagram images from
	 * every export go to the side's images folder.
	 *
	 * @param kind
	 *            what the packages are, for messages ("root node" or "package")
	 */
	private static void exportMerged(Repository repository, List<Package> packages, String kind, File eaProject,
			String side, boolean baseline, File outputDir, File xmiExportFile, DiagramXML diagramXML,
			DiagramImage diagramImage) throws Exception {
		File imagesDirectory = new File(outputDir, "Images");
		File sideImagesDirectory = new File(outputDir, "Images" + (baseline ? "-baseline" : "-destination"));
		List<File> parts = new LinkedList<File>();
		try {
			int n = 0;
			for (Package p : packages) {
				File part = new File(outputDir, baseName(xmiExportFile) + "-part" + (++n) + XMI);
				parts.add(part);
				repository.GetProjectInterface().ExportPackageXMI(p.GetPackageGUID(), EnumXMIType.xmiEA11,
						diagramXML.code(), diagramImage.code(), 1, 0, part.getAbsolutePath());
				if (diagramXML != DiagramXML.NO_EXPORT && imagesDirectory.exists())
					moveImages(imagesDirectory, sideImagesDirectory);
				System.out.println(side + " model " + kind + " '" + p.GetName() + "' exported.");
			}

			XmiMerger.Result merged = XmiMerger.merge(parts, xmiExportFile);
			if (merged.identicalCopiesDropped > 0) {
				System.out.println("\n" + merged.identicalCopiesDropped + " connector(s) between " + kind
						+ "s were exported with both " + kind + "s they connect; each is kept once.");
			}
			if (merged.duplicateCount > 0) {
				System.out.println("\nWARNING:  " + merged.duplicateCount + " element(s) are defined in more than one "
						+ kind + " of " + eaProject.getName() + "; they may be reported twice. First few: "
						+ merged.duplicateIds);
			}
		} finally {
			for (File part : parts)
				part.delete();
		}

		if (diagramXML != DiagramXML.NO_EXPORT) {
			if (sideImagesDirectory.exists()) {
				System.out.println("\n" + side + " model diagrams successfully exported as " + diagramImage.name()
						+ " images to:  \n" + sideImagesDirectory.getAbsolutePath());
			} else {
				System.err.println("ERROR:  Unable to export diagram images. Terminating EA .eap XMI export processing.");
				System.exit(1);
			}
		}
	}

	/**
	 * Moves the images EA wrote to {@code from} into {@code to}: a rename when
	 * {@code to} does not exist yet, otherwise (or when the rename fails) file by
	 * file, replacing files of the same name, after which {@code from} is removed if
	 * possible. A file that cannot be moved is an error, so the images of one model
	 * are never left where the next export writes (issue #91).
	 */
	static void moveImages(File from, File to) throws IOException {
		if (!to.exists() && from.renameTo(to))
			return;
		if (!to.isDirectory() && !to.mkdirs())
			throw new IOException("Unable to create " + to.getAbsolutePath());
		File[] files = from.listFiles();
		if (files != null)
			for (File f : files)
				java.nio.file.Files.move(f.toPath(), new File(to, f.getName()).toPath(),
						java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		from.delete(); // may stay behind, empty, while EA holds it; the next export reuses it
	}

	private static Package findPackage(String packageName, Collection<Package> packages) {
		for (Package aPackage : packages) {
			if (aPackage.GetName().equals(packageName)) {
				return aPackage;
			} else {
				Package result = findPackage(packageName, aPackage.GetPackages());
				if (result != null && result.GetName().equals(packageName)) {
					return result;
				}
			}
		}
		return null;
	}

	private static File createZipArchive(File[] fileArgs, Map<String, String> options) throws IOException {
		File zipFile = null;

		if (options.containsKey(PARAM_ZIP)) {

			File outputDir = fileArgs[0];
			//
			File baselineImagesDir = new File(outputDir, "Images-baseline");
			File destinationImagesDir = new File(outputDir, "Images-destination");
			//
			File compareLogXMLFile = fileArgs[1];
			File comparisonHTMLFile = fileArgs[2];

			zipFile = new File(outputDir, comparisonHTMLFile.getName().replace(HTML, "") + ZIP);
			FileOutputStream fos = new FileOutputStream(zipFile);

			ZipOutputStream zipOut = new ZipOutputStream(fos);

			zipFile(comparisonHTMLFile, comparisonHTMLFile.getName(), zipOut);

			if (options.containsKey(PARAM_INCLUDE_DIAGRAMS)) {
				if (baselineImagesDir.exists()) {
					zipFile(baselineImagesDir, baselineImagesDir.getName(), zipOut);
				}
				if (destinationImagesDir.exists()) {
					zipFile(destinationImagesDir, destinationImagesDir.getName(), zipOut);
				}
			}

			zipOut.close();
			fos.close();
			if (options.containsKey(PARAM_CLEANUP)) {
				// Only what this run created (issue #53).
				for (File created : createdByRun)
					deleteDirectory(created);
				ReportRenderer.enrichedFileFor(compareLogXMLFile, comparisonHTMLFile).delete();
				comparisonHTMLFile.delete();
			}
		}

		return zipFile;
	}
	
	private static void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            // Get all files and directories in the current directory
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    // Recursively delete each file/directory
                    deleteDirectory(file);
                }
            }
        }
        // Delete the current directory or file
        directory.delete();
    }

	private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
		if (fileToZip.isHidden()) {
			return;
		}
		//
		if (fileToZip.isDirectory()) {
			if (fileName.endsWith("/")) {
				zipOut.putNextEntry(new ZipEntry(fileName));
				zipOut.closeEntry();
			} else {
				zipOut.putNextEntry(new ZipEntry(fileName + "/"));
				zipOut.closeEntry();
			}
			File[] children = fileToZip.listFiles();
			for (File childFile : children) {
				zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
			}
			return;
		}
		//
		FileInputStream fis = new FileInputStream(fileToZip);
		ZipEntry zipEntry = new ZipEntry(fileName);
		zipOut.putNextEntry(zipEntry);
		byte[] bytes = new byte[1024];

		int length;
		while ((length = fis.read(bytes)) >= 0) {
			zipOut.write(bytes, 0, length);
		}
		//
		fis.close();
	}

	private static void printUsage() {
		String jar = "cim-compare-" + VERSION + ".jar";
		String[] lines = {
				"",
				"There are three ways to run cim-compare " + VERSION + " (see https://cim-compare.ucaiug.io):",
				"",
				"1. From two Enterprise Architect project files (.eap/.eapx with 32-bit Java, .qea/.qeax with 64-bit Java).",
				"   Requires a licensed EA installation; -Djava.library.path must name the folder holding SSJavaCOM.dll/SSJavaCOM64.dll.",
				"   Every root node of each project is compared unless --package is given.",
				"",
				"   Usage: java [<jvm-parameters>] -jar " + jar + " <baseline-model-file> <destination-model-file>",
				"               [<output-directory-or-html-file>] [--package=<package-names>] [--full]",
				"               [--include-diagrams] [--image-type=<image-file-extension>] [--zip] [--cleanup]",
				"",
				"   Examples:",
				"      java -Xmx4G -Djava.library.path=\"C:\\cim-compare\\ea16\" -jar " + jar + " CIM17v40.qea CIM18v16.qea \"C:\\Comparison Reports\"",
				"      java -Xmx4G -Djava.library.path=\"C:\\cim-compare\\ea16\" -jar " + jar + " CIM17v40.qea CIM18v16.qea --package=Grid --include-diagrams --zip",
				"      java -Xmx1G -Djava.library.path=\"C:\\cim-compare\\ea15\" -jar " + jar + " CIM15v33.eap CIM16v26a.eap CIM15v33_CIM16v26a.html --full",
				"      java -Xmx4G -Djava.library.path=\"C:\\cim-compare\\ea16\" -jar " + jar + " CIM16_Reference.qea CIM16_Extended.qeax --package=Model",
				"",
				"2. From two XMI 1.1 files exported from EA. Diagram images, if wanted, must already be in the",
				"   Images-baseline and Images-destination folders of the output directory.",
				"",
				"   Usage: java [<jvm-parameters>] -jar " + jar + " <baseline-model-xmi-file> <destination-model-xmi-file>",
				"               [<output-directory-or-html-file>] [--package=<package-names>] [--full]",
				"               [--include-diagrams] [--image-type=<image-file-extension>] [--zip] [--cleanup]",
				"",
				"   Examples:",
				"      java -Xmx2G -jar " + jar + " \"C:\\XMI exports\\CIM15v33.xmi\" \"C:\\XMI exports\\CIM16v26a.xmi\" \"C:\\Comparison Reports\"",
				"      java -Xmx2G -jar " + jar + " CIM15v33.xmi CIM16v26a.xmi CIM15v33_CIM16v26a.html --package=IEC62325",
				"      java -Xmx2G -jar " + jar + " CIM16_Reference.xmi CIM16_Extended.xmi --package=\"Grid,European Standards\"",
				"      java -Xmx2G -jar " + jar + " CIM15v33.xmi CIM16v26a.xmi --include-diagrams --image-type=GIF --zip --cleanup",
				"",
				"3. From a compare log (.xml) exported from an Enterprise Architect model comparison. Diagrams are not supported.",
				"",
				"   Usage: java [<jvm-parameters>] -jar " + jar + " <comparison-results-xml-file>",
				"               [<output-directory-or-html-file>] [--package=<package-names>] [--full] [--zip] [--cleanup]",
				"",
				"   Examples:",
				"      java -Xmx2G -jar " + jar + " CIM15v33_CIM16v26a_EA_Comparison_Report.xml \"C:\\Comparison Reports\"",
				"      java -Xmx2G -jar " + jar + " CIM15v33_CIM16v26a_EA_Comparison_Report.xml --package=IEC61970",
				"      java -Xmx2G -jar " + jar + " CIM15v33_CIM16v26a_EA_Comparison_Report.xml --package=IEC61970 --zip --cleanup",
				"",
				"Options:",
				"   --package=<names>    Compare (or report) only these packages and everything below them: one name, or",
				"                        several separated by commas, shown in that order. Quote the value when a name",
				"                        contains a space (--package=\"Grid,European Standards\"). Either model's name can be",
				"                        used for a renamed package (e.g. IEC61970 or Grid). Without it, every root node of",
				"                        an EA project is compared (option 1).",
				"   --full               Include identical items in the report as well as the changes.",
				"   --minimal            Accepted for 1.x command lines; no effect (only changes are reported by default).",
				"   --include-diagrams   Include changed diagrams (options 1 and 2).",
				"   --image-type=<ext>   JPG (default), GIF, PNG, BMP or EMF.",
				"   --zip                Put the report and diagram images in a ZIP archive.",
				"   --cleanup            With --zip: delete what this run created, leaving the ZIP. Input files are never deleted.",
				"" };
		for (String line : lines)
			System.err.println(line);
	}
}
