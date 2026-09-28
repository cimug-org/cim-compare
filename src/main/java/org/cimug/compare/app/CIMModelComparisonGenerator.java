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
	private static final String PARAM_MINIMAL = "minimal"; // 1.x option; accepted and ignored (minimal is now the default)
	private static final String PARAM_FULL = "full";
	private static final String PARAM_INCLUDE_DIAGRAMS = "include-diagrams";
	private static final String PARAM_ZIP = "zip";
	private static final String PARAM_CLEANUP = "cleanup";
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

	public static void main(String[] args) {

        // Set system properties for XPath limits. By setting to 0 we remove
		// the default "ceilings" which was previously causing cim-compare to 
		// crash at runtime.
        System.setProperty("jdk.xml.xpathExprGrpLimit", "0");
        System.setProperty("jdk.xml.xpathExprOpLimit", "0");
        System.setProperty("jdk.xml.xpathTotalOpLimit", "0");
        
		
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
			ReportRenderer renderer = new ReportRenderer(options.containsKey(PARAM_FULL), options.get(PARAM_PACKAGE),
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
						if ((value == null) || ("".equals(value))) {
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

				String baselineGuid = exportProject(arguments[0], true, outputDir, baselineXmi, thePackageName, null,
						diagramXML, diagramImage);
				String destinationGuid = exportProject(arguments[1], false, outputDir, destinationXmi, thePackageName,
						baselineGuid, diagramXML, diagramImage);

				if (thePackageName != null) {
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
	 * <li>No package name: the first root node is exported (with a warning when the
	 * project has more than one).</li>
	 * <li>A package name: the package of that name is exported, searching root nodes
	 * and everything below them. If there is none and {@code guidHint} is given, the
	 * package with that GUID is exported instead (a package renamed between versions).
	 * If neither is found nothing is exported and null is returned.</li>
	 * </ul>
	 *
	 * @return the GUID of the exported package, or null when nothing was exported
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
				packageToCompare = models.GetAt((short) 0);
				if (models.GetCount() > 1) {
					StringBuilder names = new StringBuilder();
					for (Package m : models)
						names.append(names.length() > 0 ? ", " : "").append(m.GetName());
					System.out.println("\nWARNING:  " + side + " project " + eaProject.getName() + " has "
							+ models.GetCount() + " root nodes (" + names + "). Only the first, '"
							+ packageToCompare.GetName() + "', is compared.");
				}
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
					imagesDirectory.renameTo(newImagesDirectory);
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
			File baselineXmiFile = (fileArgs.length == 5 ? fileArgs[3] : null);
			File destinationXmiFile = (fileArgs.length == 5 ? fileArgs[4] : null);

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
				deleteDirectory(baselineImagesDir);
				deleteDirectory(destinationImagesDir);
				if (baselineXmiFile != null)
					baselineXmiFile.delete();
				if (destinationXmiFile != null)
					destinationXmiFile.delete();
				compareLogXMLFile.delete();
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
		System.err.println();
		System.err.println("There are three possible command line usages for the CIM Model Comparison Report utility:");
		System.err.println();
		/**
		 * XML Compare Log file as input...
		 */
		System.err.println(
				"To generate an HTML report using the results file (*.xml) of an Enterprise Architect model comparison use the command line option.");
		System.err.println(
				"   Usage: java -jar cim-compare.jar <comparison-results-xml-file> [<output-directory-or-html-file>] [--package=<package-name>] [--full] [--include-diagrams] [--image-type=<image-files-extension>] [--zip] [--cleanup]");
		System.err.println();
		System.err.println("   Examples: ");
		System.err.println(
				"          java -jar cim-compare.jar \"C:\\CIM XMI exports\\CIM15v33_CIM16v26a_EA_Comparison_Report.xml\" \"C:\\Comparison Reports\\\"");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml \"C:\\Comparison Reports\\\"");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml \"C:\\Comparison Reports\\\" --package=IEC61968");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml MyComparisonReport_CIM15v33_CIM16v26a.html");
		System.err.println("          java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml --include-diagrams --image-type=gif");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml --package=IEC61970");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml --package=IEC61970 --include-diagrams --image-type=GIF --zip");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml --package=IEC61970 --include-diagrams --image-type=GIF --zip --cleanup");
		System.err.println();
		/**
		 * XMI files as input...
		 */
		System.err.println(
				"To generate a model comparison report directly from baseline and target models (XMI files) use the command line option.");
		System.err.println(
				"   Usage: java -jar cim-compare.jar <baseline-model-xmi-file> <target-model-xmi-file> [<output-directory-or-html-file>] [--package=<package-name>] [--full] [--include-diagrams] [--image-type=<image-files-extension>] [--zip] [--cleanup]");
		System.err.println();
		System.err.println("   Examples: ");
		System.err.println(
				"          java -jar cim-compare.jar \"C:\\CIM XMI exports\\CIM15v33.xmi\" \"C:\\CIM XMI exports\\CIM16v26a.xmi\" \"C:\\Comparison Reports\\\"");
		System.err
				.println("          java -jar cim-compare.jar CIM15v33.xmi CIM16v26a.xmi \"C:\\Comparison Reports\\\"");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.xmi CIM16v26a.xmi \"C:\\Comparison Reports\\CIM15v33_CIM16v26a_ComparisonReport.html\"");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.xmi CIM16v26a.xmi CIM15v33_CIM16v26a_ComparisonReport.html");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.xmi CIM16v26a.xmi CIM15v33_CIM16v26a_ComparisonReport.html --package=IEC62325");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.xmi CIM16v26a.xmi CIM15v33_CIM16v26a_ComparisonReport.html");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.xmi CIM16v26a.xmi --package=IEC62325 --include-diagrams --image-type=JPG --zip");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.xmi CIM16v26a.xmi --package=IEC62325 --include-diagrams --image-type=JPG --zip --cleanup");
		System.err.println();
		/**
		 * EAP files as input...
		 */
		System.err.println(
				"To generate a model comparison report directly from Sparx Enterprise Architect baseline and target models (.eap files) use the command line option.");
		System.err.println(
				"   Usage: java -jar cim-compare.jar <baseline-model-file> <target-model-file> [<output-directory-or-html-file>] [--package=<package-name>] [--full] [--include-diagrams] [--image-type=<image-files-extension>] [--zip] [--cleanup]");
		System.err.println();
		System.err.println("   Examples: ");
		System.err.println(
				"          java -jar cim-compare.jar \"C:\\CIM XMI exports\\CIM15v33.eap\" \"C:\\CIM XMI exports\\CIM16v26a.eapx\" \"C:\\Comparison Reports\\\"");
		System.err
				.println("          java -jar cim-compare.jar CIM15v33.xmi CIM16v26a.eap \"C:\\Comparison Reports\\\"");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.eap CIM16v26a.eap \"C:\\Comparison Reports\\CIM15v33_CIM16v26a_ComparisonReport.html\"");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.qea CIM16v26a.qea CIM15v33_CIM16v26a_ComparisonReport.html");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.eap CIM16v26a.eap CIM15v33_CIM16v26a_ComparisonReport.html --package=IEC62325");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.eapx CIM16v26a.eapx CIM15v33_CIM16v26a_ComparisonReport.html");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.eap CIM16v26a.xmi --package=IEC62325 --include-diagrams --image-type=JPG");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.eap CIM16v26a.xmi --package=IEC62325 --include-diagrams --image-type=JPG --zip");		
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.eap CIM16v26a.xmi --package=IEC62325 --include-diagrams --image-type=JPG --zip --cleanup");			
		System.err.println();
	}
}
