package org.cimug.compare.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.sparx.Collection;
import org.sparx.EnumXMIType;
import org.sparx.Package;
import org.sparx.Project;
import org.sparx.Repository;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CIMModelComparisonGenerator {

	private static final String PARAM_PACKAGE = "package";
	private static final String PARAM_MINIMAL = "minimal";
	private static final String PARAM_INCLUDE_DIAGRAMS = "include-diagrams";
	private static final String PARAM_ZIP = "zip";
	private static final String PARAM_IMAGE_TYPE = "image-type";
	private static final String ANSI = "windows-1252";
	private static final String UTF8 = "UTF-8";

	private static final String XML = ".xml";
	private static final String XMI = ".xmi";
	private static final String EAP = ".eap";
	private static final String EAPX = ".eapx";
	private static final String HTM = ".htm";
	private static final String HTML = ".html";
	private static final String ZIP = ".zip";
	private static final String CIM_MODEL_COMPARISON_XSLT = "CIM_Model_Comparison.xslt";

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

			TransformerFactory f = TransformerFactory.newInstance();
			InputStream stylesheet = ClassLoader.getSystemResourceAsStream(CIM_MODEL_COMPARISON_XSLT);
			StreamSource stylesource = new StreamSource(stylesheet);
			Transformer transformer = f.newTransformer(stylesource);

			/**
			 * Set all XSLT parameters received on the command line. These are passed in as
			 * various command line options with a leading "--". If the command line option
			 * includes an equals sign ("=") in it then we know that it is an option that
			 * has a value associated with it and which must be processed accordingly.
			 */
			for (String paramName : options.keySet()) {
				transformer.setParameter(paramName, options.get(paramName));
			}

			if (!options.containsKey(PARAM_IMAGE_TYPE)) {
				/**
				 * Behind the scenes we set a default image type. will be overridden if one is
				 * explicitly specified on the command line
				 */
				transformer.setParameter(PARAM_IMAGE_TYPE, DiagramImage.JPG.name());
			}

			transformer.setParameter(PARAM_INCLUDE_DIAGRAMS, options.containsKey(PARAM_INCLUDE_DIAGRAMS));

			DOMSource source = new DOMSource(document);

			StreamResult result = null;
			if (comparisonHTMLFile != null) {
				FileOutputStream fos = new FileOutputStream(comparisonHTMLFile);
				Writer writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8));
				result = new StreamResult(writer);
			} else {
				result = new StreamResult(System.out);
			}

			transformer.transform(source, result);

			System.out.println(
					"\nCIM model comparison report successfully generated:  \n" + comparisonHTMLFile.getAbsolutePath());

			File zipFile = createZipArchive(fileArgs, options);

			if (zipFile != null) {
				System.out.println(
						"\nCIM model comparison report ZIP archive generated:  \n" + zipFile.getAbsolutePath());
			}
		} catch (SAXException | TransformerException | ParserConfigurationException | IOException e) {
			e.printStackTrace();
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
					case PARAM_MINIMAL:
					case PARAM_INCLUDE_DIAGRAMS:
					case PARAM_ZIP:
						value = Boolean.TRUE.toString(); // All three must have to a default value of "true"...
						break;
					case PARAM_IMAGE_TYPE:
						if ((value != null) && (!"".equals(value))) {
							try {
								value = value.toUpperCase();
								DiagramImage.valueOf(value);
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
			 * When processing .EAP files and exporting diagrams we ensure that a default
			 * image type of 'JPG' is used if no image-type is explicitly specified on the
			 * command line.
			 */
			options.put(PARAM_IMAGE_TYPE, DiagramImage.JPG.name());
		}

		return options;
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
					&& (!(arg.getName().toLowerCase().endsWith(HTML) || arg.getName().toLowerCase().endsWith(HTM))
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
						|| ((arguments[0].getName().toLowerCase().endsWith(EAP)
								|| arguments[0].getName().toLowerCase().endsWith(EAPX))
								&& (arguments[1].getName().toLowerCase().endsWith(EAP)
										|| arguments[1].getName().toLowerCase().endsWith(EAPX))))) //
						|| (!arguments[2].isDirectory() && !arguments[2].getName().toLowerCase().endsWith(HTML)
								&& !arguments[2].getName().toLowerCase().endsWith(HTM))))) {
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

			/**
			 * Determine the output directory...
			 */
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
			 * now transition to testing if the input files are .EAP files which require the
			 * additional step of first exporting the baseline and target XMI files before
			 * executing a call to the DiffXMLGenerator.
			 */
			if (arguments[0].getName().toLowerCase().endsWith(EAP)
					|| arguments[0].getName().toLowerCase().endsWith(EAPX)) {

				DiagramXML diagramXML = (options.containsKey(PARAM_INCLUDE_DIAGRAMS) ? DiagramXML.EXPORT_WITHOUT_IMAGES
						: DiagramXML.NO_EXPORT);

				DiagramImage diagramImage = (options.containsKey(PARAM_IMAGE_TYPE)
						? DiagramImage.valueOf(options.get(PARAM_IMAGE_TYPE))
						: DiagramImage.NONE);

				String thePackageName = (options.containsKey(PARAM_PACKAGE) ? options.get(PARAM_PACKAGE) : null);

				/**
				 * Note that argument index 0 corresponds to the 'baseline' file on the command
				 * line while argument index 1 is for the 'target' file.
				 */
				for (int index = 0; index < 2; index++) {

					Repository repository = null;
					boolean eapFailed = false;

					try {
						String eapFile = arguments[index].getAbsolutePath();
						String eapFileName = arguments[index].getName().substring(0,
								arguments[index].getName().lastIndexOf("."));

						repository = new Repository();
						repository.OpenFile(eapFile);

						Collection<Package> packages = repository.GetModels();

						Package rootModelPackage = packages.GetAt((short) 0);

						Project project = repository.GetProjectInterface();

						Package packageToCompare = null;

						if (thePackageName == null) {
							packageToCompare = rootModelPackage;
						} else {
							Collection<Package> childPackages = rootModelPackage.GetPackages();
							packageToCompare = findPackage(thePackageName, childPackages);
							if (packageToCompare == null) {
								packageToCompare = rootModelPackage;
							}
						}

						File xmiExportFile = new File(outputDir, eapFileName + XMI);

						if (index == 0) {
							baselineXmiFile = xmiExportFile.getAbsolutePath();
						} else {
							destinationXmiFile = xmiExportFile.getAbsolutePath();
						}

						project.ExportPackageXMI(packageToCompare.GetPackageGUID(), EnumXMIType.xmiEA11,
								diagramXML.code(), diagramImage.code(), 1, 0, xmiExportFile.getAbsolutePath());

						if (diagramXML != DiagramXML.NO_EXPORT) {
							File imagesDirectory = new File(outputDir, "Images");
							if (imagesDirectory.exists()) {
								File newImagesDirectory = new File(outputDir,
										"Images" + (index == 0 ? "-baseline" : "-destination"));
								imagesDirectory.renameTo(newImagesDirectory);

								System.out.println("\n" + (index == 0 ? "Baseline" : "Destination")
										+ " model diagrams successfuly exported as " + diagramImage.name()
										+ " images to:  \n" + newImagesDirectory.getAbsolutePath());
							} else {
								System.err.println(
										"ERROR:  Unable to export diagram images. Terminating EA .eap XMI export processing.");
								System.exit(1);
							}
						}

						System.out.println("\n" + (index == 0 ? "Baseline" : "Destination")
								+ " model XMI export completed successfully:  \n" + xmiExportFile.getAbsolutePath());
					} catch (Exception e) {
						eapFailed = true;
						e.printStackTrace();
					} finally {
						// We must explicitly make a GC call. This is due to a limitation in EA's Java
						// API and memory...
						System.gc();
						if (repository != null) {
							/**
							 * The following is required by EA's automation API's and ensures that
							 * everything properly terminates...
							 */
							repository.CloseFile();
							repository.Exit();
							repository = null;
						}

						// If an exception occurred we want to exit processing...
						if (eapFailed) {
							System.err.println(
									"ERROR:  Terminating EA .eap XMI export processing due to an unexpected exception.");
							System.err.println();
							System.exit(1);
						}
					}
				}
			} else {
				// We have determined that the two input files are XMI files so we simply
				// set the baselineXMIInputFiles & targetXMIInputFiles variables to the
				// values of arguments[0] and arguments[1] respectively.
				baselineXmiFile = arguments[0].getAbsolutePath();
				destinationXmiFile = arguments[1].getAbsolutePath();
			}

			DiffXMLGenerator.main(
					new String[] { baselineXmiFile, destinationXmiFile, modelComparisonXMLFile.getAbsolutePath(), (options.containsKey(PARAM_IMAGE_TYPE) ? options.get(PARAM_IMAGE_TYPE) : DiagramImage.JPG.toString())});

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

			if (options.containsKey(PARAM_INCLUDE_DIAGRAMS) && baselineImagesDir.exists()) {
				zipFile(baselineImagesDir, baselineImagesDir.getName(), zipOut);
			}

			if (options.containsKey(PARAM_INCLUDE_DIAGRAMS) && destinationImagesDir.exists()) {
				zipFile(destinationImagesDir, destinationImagesDir.getName(), zipOut);
			}

			/**
			 * Currently we are not packaging these files in order to keep the ZIP archive
			 * small
			 *
			 * if (baselineXmiFile.exists()) { zipFile(baselineXmiFile,
			 * baselineXmiFile.getName(), zipOut); } if (destinationXmiFile.exists()) {
			 * zipFile(destinationXmiFile, destinationXmiFile.getName(), zipOut); }
			 * zipFile(compareLogXMLFile, compareLogXMLFile.getName(), zipOut);
			 */
			zipOut.close();
			fos.close();
		}

		return zipFile;
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
				"   Usage: java -jar cim-compare.jar <comparison-results-xml-file> [<output-directory-or-html-file>] [--package=<iec-package-name>] [--minimal] [--include-diagrams] [--image-type=<image-files-extension>] [--zip]");
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
				"          java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml --include-diagrams --image-type=gif --minimal");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml --package=IEC61970");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml --package=IEC61970 --include-diagrams --image-type=GIF --zip");
		System.err.println();
		/**
		 * XMI files as input...
		 */
		System.err.println(
				"To generate a model comparison report directly from baseline and target models (XMI files) use the command line option.");
		System.err.println(
				"   Usage: java -jar cim-compare.jar <baseline-model-xmi-file> <target-model-xmi-file> [<output-directory-or-html-file>] [--package=<iec-package-name>]  [--minimal] [--include-diagrams] [--image-type=<image-files-extension>] [--zip]");
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
				"          java -jar cim-compare.jar CIM15v33.xmi CIM16v26a.xmi CIM15v33_CIM16v26a_ComparisonReport.html --minimal");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.xmi CIM16v26a.xmi --package=IEC62325 --minimal --include-diagrams --image-type=JPG --zip");
		System.err.println();
		/**
		 * EAP files as input...
		 */
		System.err.println(
				"To generate a model comparison report directly from Sparx Enterprise Architect baseline and target models (.eap files) use the command line option.");
		System.err.println(
				"   Usage: java -jar cim-compare.jar <baseline-model-eap-file> <target-model-eap-file> [<output-directory-or-html-file>] [--package=<iec-package-name>] [--minimal] [--include-diagrams] [--image-type=<image-files-extension>] [--zip]");
		System.err.println();
		System.err.println("   Examples: ");
		System.err.println(
				"          java -jar cim-compare.jar \"C:\\CIM XMI exports\\CIM15v33.eap\" \"C:\\CIM XMI exports\\CIM16v26a.eapx\" \"C:\\Comparison Reports\\\"");
		System.err
				.println("          java -jar cim-compare.jar CIM15v33.xmi CIM16v26a.eap \"C:\\Comparison Reports\\\"");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.eap CIM16v26a.eap \"C:\\Comparison Reports\\CIM15v33_CIM16v26a_ComparisonReport.html\"");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.eap CIM16v26a.eap CIM15v33_CIM16v26a_ComparisonReport.html");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.eap CIM16v26a.eap CIM15v33_CIM16v26a_ComparisonReport.html --package=IEC62325");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.eapx CIM16v26a.eapx CIM15v33_CIM16v26a_ComparisonReport.html --minimal");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33.eap CIM16v26a.xmi --package=IEC62325 --include-diagrams --image-type=JPG --minimal");
		System.err.println();
	}
}
