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
	private static final String PARAM_SHOW_EMPTY_VALUES = "show-empty-values";
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
		NONE(-1, null), EMF(0, "emf"), BMP(1, "bmp"), GIF(2, "gif"), PNG(3, "png"), JPG(4, "jpg"),
		/**
		 * #139: EA's XMI export cannot write SVG, so the XMI is exported without images
		 * (code -1) and an SVG of each diagram is then written with
		 * PutDiagramImageToFile (EA 16.1 build 1629 and later).
		 */
		SVG(-1, "svg");

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
			renderer.showEmptyValues(options.containsKey(PARAM_SHOW_EMPTY_VALUES));
			if (svgFellBack)
				renderer.imageNote("Diagrams are JPG: SVG images were not available from this version of EA.");
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
	/**
	 * The --image-type values, e.g. "EMF, BMP, GIF, PNG, JPG" (#139: the loop that built this list
	 * used to run one past the end and crash on an invalid value instead of printing it).
	 */
	static String validImageTypes() {
		String valid = "";
		for (DiagramImage type : DiagramImage.values())
			if (type != DiagramImage.NONE)
				valid += (valid.isEmpty() ? "" : ", ") + type.name();
		return valid;
	}

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
					case PARAM_SHOW_EMPTY_VALUES:
					case PARAM_INCLUDE_DIAGRAMS:
					case PARAM_ZIP:
					case PARAM_CLEANUP:
						value = Boolean.TRUE.toString(); // flags carry no value...
						break;
					case PARAM_IMAGE_TYPE:
						if ((value != null) && (!"".equals(value))) {
							try {
								DiagramImage.valueOf(value.toUpperCase());
								if ("EMF".equalsIgnoreCase(value))
									System.out.println("WARNING:  Browsers cannot show EMF images, so the report's diagrams will"
											+ " not display. Use SVG, PNG or JPG for the HTML report.");
								// extensions on images must be lower case...
								value = value.toLowerCase(); 
							} catch (Exception e) {
								System.err.println(PARAM_IMAGE_TYPE + " must be one of: " + validImageTypes());

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

				svgUnavailable = null;
				svgFellBack = false;
				svgUsed = false;
				eaReported = false;
				DiagramImage diagramImage = (options.containsKey(PARAM_IMAGE_TYPE)
						? DiagramImage.valueOf(options.get(PARAM_IMAGE_TYPE).toUpperCase())
						: DiagramImage.NONE);

				String thePackageName = (options.containsKey(PARAM_PACKAGE) ? options.get(PARAM_PACKAGE) : null);

				/**
				 * Export the baseline (argument 0) and destination (argument 1) projects.
				 *
				 * Without --package every root node is compared (#88). With --package (one
				 * name or a comma list, #94) each package is located by name in each project,
				 * root nodes included; a package renamed between the two versions (e.g.
				 * IEC61970 -> Grid) is found by name in one project and by GUID in the other
				 * (#49). A package found in only one project is reported as added or
				 * deleted; a name found in neither stops the run.
				 */
				File baselineXmi = new File(outputDir, baseName(arguments[0]) + XMI);
				File destinationXmi = new File(outputDir, baseName(arguments[1]) + XMI);

				selectedPackages = exportBoth(arguments, outputDir, baselineXmi, destinationXmi, thePackageName,
						diagramXML, diagramImage);

				if (diagramImage == DiagramImage.SVG && diagramXML != DiagramXML.NO_EXPORT && svgUnavailable != null) {
					// #139: SVG images could not be had from this EA; the run uses JPG images
					if (svgUsed) {
						// an SVG failed after a model was exported for SVG (without images): export
						// both models again, with JPG images
						System.out.println("\nExporting both models again with JPG diagram images.");
						clearImages(outputDir);
						selectedPackages = exportBoth(arguments, outputDir, baselineXmi, destinationXmi, thePackageName,
								diagramXML, DiagramImage.JPG);
					}
					options.put(PARAM_IMAGE_TYPE, DiagramImage.JPG.ext());
					svgFellBack = true;
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
	 * Exports the baseline (argument 0) and destination (argument 1) projects; see
	 * the comment where it is called. Returns the --package selection, or null.
	 */
	private static List<String> exportBoth(File[] arguments, File outputDir, File baselineXmi, File destinationXmi,
			String thePackageName, DiagramXML diagramXML, DiagramImage diagramImage) {
		if (thePackageName == null) {
			exportProject(arguments[0], true, outputDir, baselineXmi, diagramXML, diagramImage);
			exportProject(arguments[1], false, outputDir, destinationXmi, diagramXML, diagramImage);
			return null;
		}
		String whole = PackageNames.isList(thePackageName) ? thePackageName.trim() : null;
		return exportPackages(arguments, outputDir, baselineXmi, destinationXmi, whole, PackageNames.parse(thePackageName),
				diagramXML, diagramImage);
	}

	/** #139: empties the two image folders before the models are exported again. */
	private static void clearImages(File outputDir) {
		for (String dir : new String[] { "Images-baseline", "Images-destination" }) {
			File[] files = new File(outputDir, dir).listFiles();
			if (files != null)
				for (File f : files)
					f.delete();
		}
	}

	/**
	 * Opens an EA project and exports its root node to XMI 1.1 (plus diagram images
	 * when requested), the comparison without --package. When the project has more
	 * than one root node, each is exported and the exports are merged into one XMI
	 * file (see {@link #exportAllRoots}, issue #88). --package is handled by
	 * {@link #exportPackages}.
	 *
	 * @return the GUID of the exported root node (the first when several are merged)
	 */
	private static String exportProject(File eaProject, boolean baseline, File outputDir, File xmiExportFile,
			DiagramXML diagramXML, DiagramImage diagramImage) {
		String side = baseline ? "Baseline" : "Destination";
		Repository repository = null;
		boolean failed = false;
		try {
			repository = new Repository();
			repository.OpenFile(eaProject.getAbsolutePath());
			diagramImage = effectiveImage(repository, diagramImage, diagramXML);

			Collection<Package> models = repository.GetModels();
			if (models.GetCount() > 1)
				return exportAllRoots(repository, models, eaProject, side, baseline, outputDir, xmiExportFile,
						diagramXML, diagramImage);
			Package packageToCompare = models.GetAt((short) 0);

			repository.GetProjectInterface().ExportPackageXMI(packageToCompare.GetPackageGUID(), EnumXMIType.xmiEA11, diagramXML.code(),
					diagramImage.code(), 1, 0, xmiExportFile.getAbsolutePath());

			if (diagramXML != DiagramXML.NO_EXPORT && diagramImage == DiagramImage.SVG) {
				writeSvgImages(repository, xmiExportFile, new File(outputDir, "Images" + (baseline ? "-baseline" : "-destination")), side);
			} else if (diagramXML != DiagramXML.NO_EXPORT) {
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
				closeQuietly(repository);
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

	/** The packages of one project that the --package names resolve to (#94). */
	private static final class Selection {
		/** The whole --package value is the name of one package (a name containing a comma). */
		boolean wholeName;
		/** The packages found were exported. */
		boolean exported;
		/** GUID of each name's package in this project, or null when not found. */
		Map<String, String> guids = new java.util.LinkedHashMap<String, String>();
	}

	/**
	 * Exports the packages named with --package from both projects (issues #49,
	 * #94). Each name is looked up by name in each project, root nodes included,
	 * and by GUID in the other project when the package was renamed. A package
	 * found in only one project is reported as added or deleted (the other side
	 * exports without it, or as an empty model); a name found in neither stops the
	 * run. A package inside another listed package is dropped with a warning.
	 *
	 * @param whole
	 *            the whole --package value when it contains a comma, to check first
	 *            whether it is the name of one package; otherwise null
	 * @return the names compared, in the order given
	 */
	private static List<String> exportPackages(File[] arguments, File outputDir, File baselineXmi,
			File destinationXmi, String whole, List<String> names, DiagramXML diagramXML, DiagramImage diagramImage) {
		Set<String> nested = new java.util.LinkedHashSet<String>();
		// 1. Baseline, by name; exported now if every name is found.
		Selection b = selectPackages(arguments[0], true, outputDir, baselineXmi, whole, names, null, false, nested,
				diagramXML, diagramImage);
		if (b.wholeName)
			return exportPackages(arguments, outputDir, baselineXmi, destinationXmi, null,
					java.util.Collections.singletonList(whole), diagramXML, diagramImage);
		// 2. Destination, by name or by the baseline's GUID.
		Selection d = selectPackages(arguments[1], false, outputDir, destinationXmi, b.exported ? null : whole, names,
				b.guids, true, nested, diagramXML, diagramImage);
		if (d.wholeName)
			return exportPackages(arguments, outputDir, baselineXmi, destinationXmi, null,
					java.util.Collections.singletonList(whole), diagramXML, diagramImage);
		if (!d.exported) {
			for (String name : names)
				if (b.guids.get(name) == null && d.guids.get(name) == null)
					System.err.println("ERROR:  Package '" + name
							+ "' was not found in either the baseline or the destination model.");
			System.exit(1);
		}
		// 3. Baseline again when a name was not found there by name: by the
		// destination's GUID (a renamed package); a package only in the destination
		// is left out.
		if (!b.exported)
			b = selectPackages(arguments[0], true, outputDir, baselineXmi, null, names, d.guids, true, nested,
					diagramXML, diagramImage);

		List<String> compared = new LinkedList<String>();
		for (String name : names) {
			if (nested.contains(name))
				continue;
			compared.add(name);
			String bg = b.guids.get(name), dg = d.guids.get(name);
			if (bg == null)
				System.out.println("\nPackage '" + name + "' is only in the destination model; it is reported as added.");
			else if (dg == null)
				System.out.println("\nPackage '" + name + "' is only in the baseline model; it is reported as deleted.");
			else if (!bg.equals(dg))
				System.out.println("\nWARNING:  The packages named '" + name
						+ "' in the baseline and destination models have different GUIDs (" + bg + ", " + dg
						+ "). They are compared as different packages.");
		}
		return compared;
	}

	/**
	 * Opens an EA project and finds the package for each name: by name (root nodes
	 * included), else by the GUID {@code hints} gives for that name. The packages
	 * are exported to {@code xmiExportFile} (merged when there are several, an
	 * empty model when there are none) when every name is found or, with
	 * {@code exportFound}, when each missing name has a GUID from the other project
	 * (it is in that project only). Packages inside another listed package are
	 * left out and recorded in {@code nested}, with a warning the first time.
	 */
	private static Selection selectPackages(File eaProject, boolean baseline, File outputDir, File xmiExportFile,
			String whole, List<String> names, Map<String, String> hints, boolean exportFound, Set<String> nested,
			DiagramXML diagramXML, DiagramImage diagramImage) {
		String side = baseline ? "Baseline" : "Destination";
		Selection sel = new Selection();
		Repository repository = null;
		boolean failed = false;
		try {
			repository = new Repository();
			repository.OpenFile(eaProject.getAbsolutePath());
			diagramImage = effectiveImage(repository, diagramImage, diagramXML);
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
			if (found.size() < names.size()) {
				if (!exportFound)
					return sel;
				for (String name : names)
					if (!found.containsKey(name) && (hints == null || hints.get(name) == null))
						return sel; // found in neither project
			}

			// A package inside another listed package is reported as part of it.
			Set<String> listed = new HashSet<String>();
			for (Package p : found.values())
				listed.add(p.GetPackageGUID());
			List<Package> packages = new LinkedList<Package>();
			List<String> exportedNames = new LinkedList<String>();
			for (String name : names) {
				Package p = found.get(name);
				if (p == null || nested.contains(name))
					continue;
				Package outer = listedAncestor(repository, p, listed);
				if (outer != null) {
					if (nested.add(name))
						System.out.println("\nWARNING:  Package '" + name + "' is inside '" + outer.GetName()
								+ "', which is also given with --package; it is reported as part of '" + outer.GetName()
								+ "'.");
					continue;
				}
				exportedNames.add(name);
				packages.add(p);
			}

			if (packages.isEmpty()) {
				XmiMerger.writeEmpty(xmiExportFile);
				System.out.println("\n" + side + " model: none of the packages is in this model; it is compared as empty.");
			} else if (packages.size() == 1) {
				repository.GetProjectInterface().ExportPackageXMI(packages.get(0).GetPackageGUID(),
						EnumXMIType.xmiEA11, diagramXML.code(), diagramImage.code(), 1, 0,
						xmiExportFile.getAbsolutePath());
				File imagesDirectory = new File(outputDir, "Images");
				File sideImages = new File(outputDir, "Images" + (baseline ? "-baseline" : "-destination"));
				if (diagramXML != DiagramXML.NO_EXPORT && diagramImage == DiagramImage.SVG) {
					writeSvgImages(repository, xmiExportFile, sideImages, side);
				} else if (diagramXML != DiagramXML.NO_EXPORT) {
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
						+ PackageNames.join(exportedNames) + ", merged):  \n" + xmiExportFile.getAbsolutePath());
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
				closeQuietly(repository);
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

		if (diagramXML != DiagramXML.NO_EXPORT && diagramImage == DiagramImage.SVG) {
			writeSvgImages(repository, xmiExportFile, sideImagesDirectory, side);
		} else if (diagramXML != DiagramXML.NO_EXPORT) {
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
	 * Closes EA. #139: after an EA call fails (e.g. "Internal application error" from
	 * PutDiagramImageToFile), EA can report the same error again on CloseFile or Exit;
	 * that must not stop the run, whose export is already written.
	 */
	private static void closeQuietly(Repository repository) {
		try {
			repository.CloseFile();
		} catch (Throwable e) {
			System.out.println("\nNote:  EA reported an error while closing the project (" + e.getMessage() + "); continuing.");
		}
		try {
			repository.Exit();
		} catch (Throwable e) {
			// already reported, or EA has gone
		}
	}

	/** #139: why SVG images could not be had in this run (null while they could). */
	private static String svgUnavailable;
	/** #139: SVG was asked for but JPG images were used. */
	private static boolean svgFellBack;
	/** #139: a model was exported for SVG images (without images) in this run. */
	private static boolean svgUsed;
	/** EA's build has been printed in this run. */
	private static boolean eaReported;

	/** #139: the first EA build whose PutDiagramImageToFile writes SVG (EA 16.1 build 1629). */
	static final int SVG_MIN_BUILD = 1629;

	/**
	 * Prints EA's build and edition (once a run) and returns the image type to export
	 * with: #139 SVG becomes JPG when this EA cannot write SVG images (before build
	 * 1629) or an SVG has already failed in this run.
	 */
	private static DiagramImage effectiveImage(Repository repository, DiagramImage requested, DiagramXML diagramXML) {
		int build = -1;
		try {
			build = repository.GetLibraryVersion();
			if (!eaReported) {
				String edition = "";
				try {
					edition = ", edition " + repository.GetEAEditionEx();
				} catch (Throwable ignored) {
					// older EA
				}
				System.out.println("\nEnterprise Architect build " + build + " (" + eaVersion(build) + ")" + edition + ".");
				eaReported = true;
			}
		} catch (Throwable ignored) {
			// unknown build: SVG is tried, with the JPG fallback
		}
		if (requested != DiagramImage.SVG || diagramXML == DiagramXML.NO_EXPORT)
			return requested;
		if (svgUnavailable == null && build >= 0 && build < SVG_MIN_BUILD) {
			svgUnavailable = "SVG images need Enterprise Architect 16.1 build " + SVG_MIN_BUILD + " or later; this is build "
					+ build;
			System.out.println("\nWARNING:  " + svgUnavailable + ". The report uses JPG images instead.");
		}
		return svgUnavailable == null ? DiagramImage.SVG : DiagramImage.JPG;
	}

	/** The EA version a build belongs to, as far as can be told from its number. */
	static String eaVersion(int build) {
		if (build >= 1700)
			return "EA 17";
		if (build >= 1620)
			return "EA 16.1";
		if (build >= 1600)
			return "EA 16.0";
		if (build >= 1500)
			return "EA 15";
		return "EA before 15";
	}

	/** #139: the diagrams in an exported XMI file, by their xmi.id (EAID_...). */
	static List<String> diagramIds(File xmi) throws IOException {
		String text = new String(java.nio.file.Files.readAllBytes(xmi.toPath()), java.nio.charset.StandardCharsets.ISO_8859_1);
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("<UML:Diagram\\s[^>]*?xmi\\.id=\"(EAID_[^\"]+)\"").matcher(text);
		java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<String>();
		while (m.find())
			ids.add(m.group(1));
		return new java.util.ArrayList<String>(ids);
	}

	/**
	 * #139: writes an SVG of each diagram in a model's exported XMI file, with EA's
	 * {@code Project.PutDiagramImageToFile} (Type 1: the format from the file name). A
	 * diagram EA will not draw as SVG is written as a PNG instead (shown without
	 * highlights); only when not one SVG can be written does the run fall back to JPG.
	 */
	private static void writeSvgImages(Repository repository, File xmi, File imagesDir, String side) {
		svgUsed = true;
		if (svgUnavailable != null)
			return;
		List<String> ids;
		try {
			ids = diagramIds(xmi);
		} catch (IOException e) {
			svgUnavailable = "could not read " + xmi.getName() + ": " + e.getMessage();
			return;
		}
		imagesDir.mkdirs();
		svgRetries = 0;
		int written = 0;
		List<String> png = new java.util.ArrayList<String>(), none = new java.util.ArrayList<String>();
		for (String id : ids) {
			String reason = writeSvg(repository, id, new File(imagesDir, id + ".svg"));
			if (reason == null) {
				written++;
				continue;
			}
			// #139: one diagram EA will not draw as SVG: show it as a PNG (without highlights)
			// and keep the other diagrams' SVGs
			String name = diagramName(repository, id);
			if (writePng(repository, id, new File(imagesDir, id + ".png"))) {
				png.add(name + id);
				System.out.println("\nWARNING:  " + side + " model: EA could not draw diagram " + name + id + " as SVG ("
						+ reason + "); it is shown as a PNG image, without highlights.");
			} else {
				none.add(name + id);
				System.out.println("\nWARNING:  " + side + " model: EA could not draw diagram " + name + id
						+ " as SVG or PNG (" + reason + "); the report shows no image of it.");
			}
		}
		if (written == 0 && !ids.isEmpty()) {
			// not one SVG: this EA cannot write them after all; the run uses JPG images
			svgUnavailable = "EA did not write any SVG image";
			return;
		}
		System.out.println("\n" + side + " model: " + written + " diagram(s) written as SVG images"
				+ (svgRetries > 0 ? " (EA needed " + svgRetries + " extra tr" + (svgRetries == 1 ? "y" : "ies") + ")" : "")
				+ (png.isEmpty() ? "" : ", " + png.size() + " as PNG") + (none.isEmpty() ? "" : ", " + none.size() + " without an image")
				+ " to:  \n" + imagesDir.getAbsolutePath());
	}

	/** #139: a PNG of one diagram, for one EA would not draw as SVG; true when written. */
	private static boolean writePng(Repository repository, String eaid, File png) {
		for (int attempt = 1; attempt <= 2; attempt++) {
			try {
				if (attempt > 1)
					Thread.sleep(1000);
				png.delete();
				repository.GetProjectInterface().PutDiagramImageToFile(attempt == 1 ? eaidToGuid(eaid) : eaid,
						png.getAbsolutePath(), 1);
				if (png.isFile() && png.length() > 8)
					return true;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			} catch (Throwable e) {
				// try again, then give up
			}
		}
		png.delete();
		return false;
	}

	/** #139: SVGs of the current model that EA wrote only on a second try. */
	private static int svgRetries;

	/**
	 * Writes one diagram's SVG; null when it worked, else what EA said on each try.
	 * EA 17 sometimes fails on a diagram it draws fine a moment later ("Internal
	 * application error", or false with no file), so there are three tries: the GUID,
	 * the GUID in its XML form after a short pause, and loading the diagram and saving
	 * its image after a longer one.
	 */
	private static String writeSvg(Repository repository, String eaid, File svg) {
		List<String> tries = new java.util.ArrayList<String>();
		for (int attempt = 1; attempt <= 3; attempt++) {
			try {
				if (attempt > 1) {
					Thread.sleep(attempt == 2 ? 500 : 2000);
					svgRetries++;
				}
				svg.delete();
				String said = "";
				boolean ok;
				if (attempt == 1) {
					ok = repository.GetProjectInterface().PutDiagramImageToFile(eaidToGuid(eaid), svg.getAbsolutePath(), 1);
				} else if (attempt == 2) {
					ok = repository.GetProjectInterface().PutDiagramImageToFile(eaid, svg.getAbsolutePath(), 1);
				} else {
					repository.GetProjectInterface().LoadDiagram(eaid);
					said = repository.GetProjectInterface().SaveDiagramImageToFile(svg.getAbsolutePath());
					ok = svg.isFile();
				}
				if (!ok || !svg.isFile()) {
					tries.add("try " + attempt + ": no file" + lastError(repository, said));
					continue;
				}
				String text = new String(java.nio.file.Files.readAllBytes(svg.toPath()), java.nio.charset.StandardCharsets.UTF_8);
				if (!org.cimug.compare.SvgDiagrams.isSvg(text)) {
					tries.add("try " + attempt + ": not an SVG");
					continue;
				}
				java.nio.file.Files.write(svg.toPath(),
						org.cimug.compare.SvgDiagrams.withFontFallbacks(text).getBytes(java.nio.charset.StandardCharsets.UTF_8));
				return null;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				tries.add("interrupted");
				break;
			} catch (Throwable e) {
				tries.add("try " + attempt + ": " + e.getMessage());
			}
		}
		svg.delete();
		return String.join("; ", tries);
	}

	/** EA's last error message, if it has one, for the messages of {@link #writeSvg}. */
	private static String lastError(Repository repository, String said) {
		String err = said == null ? "" : said.trim();
		try {
			String e = repository.GetProjectInterface().GetLastError();
			if (e != null && !e.trim().isEmpty())
				err += (err.isEmpty() ? "" : " / ") + e.trim();
		} catch (Throwable ignored) {
			// none
		}
		return err.isEmpty() ? "" : " (" + err + ")";
	}

	/** "'Name' " of a diagram, for messages; empty when EA cannot say. */
	private static String diagramName(Repository repository, String eaid) {
		try {
			return "'" + repository.GetDiagramByGuid(eaidToGuid(eaid)).GetName() + "' ";
		} catch (Throwable e) {
			return "";
		}
	}

	/** EAID_70EC2267_19ED_48a1_973A_CD797502D5A8 -> {70EC2267-19ED-48a1-973A-CD797502D5A8}. */
	static String eaidToGuid(String eaid) {
		return "{" + eaid.substring(eaid.startsWith("EAID_") ? 5 : 0).replace('_', '-') + "}";
	}

	/** For tests: as if an SVG could not be written. */
	static void svgUnavailable(String reason) {
		svgUnavailable = reason;
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
				"               [<output-directory-or-html-file>] [--package=<package-names>] [--full] [--show-empty-values]",
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
				"               [<output-directory-or-html-file>] [--package=<package-names>] [--full] [--show-empty-values]",
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
				"               [<output-directory-or-html-file>] [--package=<package-names>] [--full] [--show-empty-values] [--zip] [--cleanup]",
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
				"   --show-empty-values  Also report a value that is empty on one side in EA housekeeping fields (Complexity,",
				"                        Phase, Version, GenType; Containment \"Not Specified\"; IsLiteral, Derived, Const,",
				"                        Static false). Left out by default; the report says how many.",
				"   --minimal            Accepted for 1.x command lines; no effect (only changes are reported by default).",
				"   --include-diagrams   Include changed diagrams (options 1 and 2).",
				"   --image-type=<ext>   JPG (default), SVG, PNG, GIF, BMP or EMF. SVG needs an EA that can write",
				"                        SVG diagram images (EA 17); otherwise JPG is used. Browsers cannot show EMF.",
				"   --zip                Put the report and diagram images in a ZIP archive.",
				"   --cleanup            With --zip: delete what this run created, leaving the ZIP. Input files are never deleted.",
				"" };
		for (String line : lines)
			System.err.println(line);
	}
}
