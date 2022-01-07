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
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CIMModelComparisonGenerator {

	private static final String PARAM_PACKAGE = "package";
	private static final String PARAM_MINIMAL = "minimal";
	private static final String PARAM_IMAGE_TYPE = "image-type";
	private static final String ANSI = "windows-1252";
	private static final String UTF8 = "UTF-8";

	private static final String XML = ".xml";
	private static final String XMI = ".xmi";
	private static final String HTM = ".htm";
	private static final String HTML = ".html";
	private static final String CIM_MODEL_COMPARISON_XSLT = "CIM_Model_Comparison.xslt";

	public static void main(String[] args) {

		File[] fileArgs = parseFileArguments(args);

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

			File datafile = fileArgs[0];
			File outputfile = fileArgs[1];

			DocumentBuilder builder = null;
			Document document = null;
			InputSource is = null;

			try {
				builder = factory.newDocumentBuilder();
				is = new InputSource(new FileInputStream(datafile));
				is.setEncoding(UTF8);
				document = builder.parse(is);
			} catch (Exception e) {
				try {
					builder = factory.newDocumentBuilder();
					document = builder.parse(datafile);
				} catch (Exception e2) {
					builder = factory.newDocumentBuilder();
					is = new InputSource(new FileInputStream(datafile));
					is.setEncoding(ANSI);
					document = builder.parse(is);
				}
			}

			TransformerFactory f = TransformerFactory.newInstance();
			InputStream stylesheet = ClassLoader.getSystemResourceAsStream(CIM_MODEL_COMPARISON_XSLT);
			StreamSource stylesource = new StreamSource(stylesheet);
			Transformer transformer = f.newTransformer(stylesource);

			/** We set the default image type. May be overridden if one is explicitly specified on the command line */
			transformer.setParameter(PARAM_IMAGE_TYPE, "jpg");
			
			/**
			 * Set all XSLT parameters received on the command line. These are passed in as
			 * various command line options with a leading "--". If the command line option
			 * includes an equals sign ("=") in it then we know that it is an option that
			 * has a value associated with it and which must be processed accordingly.
			 */
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
							value = "true"; // defaults to true
							break;
						case PARAM_IMAGE_TYPE:
							if ((value == null) || ("".equals(value))) {
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

					transformer.setParameter(param, value);
				}
			}

			DOMSource source = new DOMSource(document);

			StreamResult result = null;
			if (outputfile != null) {
				FileOutputStream fos = new FileOutputStream(outputfile);
		        Writer writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8));
				result = new StreamResult(writer);
			} else {
				result = new StreamResult(System.out);
			}

			transformer.transform(source, result);

			System.out.println(
					"\nCIM model comparision report successfully generated:  \n" + outputfile.getAbsolutePath());
		} catch (SAXException | TransformerException | ParserConfigurationException | IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param option The command line option that was invalid.
	 */
	private static void exitOnInvalidParameter(String option) {
		System.err.print("Invalid command line option specified: " + option);
		System.err.println();
		printUsage();
		System.exit(1);
	}

	private static File[] parseFileArguments(String[] args) {

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
				if (!(argument.getName().toLowerCase().endsWith(HTML) || argument.getName().toLowerCase().endsWith(HTM))
						&& !argument.exists()) {
					// MUST be specified using the getPath() method. This method
					// will properly reflect what was passed on the command line.
					System.err.println("   " + argument.getPath());
				}
			}
			System.exit(1);
		} else if ((arguments.length == 1
				&& (arguments[0].isDirectory() || !arguments[0].getName().toLowerCase().endsWith(XML))) || //
				(arguments.length >= 1 && arguments[0].isDirectory()) || //
				(arguments.length == 2 && ((arguments[0].getName().toLowerCase().endsWith(XMI)
						&& !arguments[1].getName().toLowerCase().endsWith(XMI))
						|| (arguments[0].getName().toLowerCase().endsWith(XML)
								&& (!(arguments[1].getName().toLowerCase().endsWith(HTML)
										|| arguments[1].getName().toLowerCase().endsWith(HTM)
										|| arguments[1].isDirectory())))))
				|| //
				(arguments.length > 2 && (!arguments[0].getName().toLowerCase().endsWith(XMI)
						|| !arguments[1].getName().toLowerCase().endsWith(XMI)
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
		} else {
			// At this point we've vetted out the validity of the arguments and that they
			// exist on the file system. We now determine if a call to the DiffXMLGenerator
			// is needed.

			if (arguments[0].getName().toLowerCase().endsWith(XMI)) {
				String defaultComparisonXMLFileName = "CIMModelComparison_" + arguments[0].getName().replace(XMI, "")
						+ "_AND_" + arguments[1].getName().replace(XMI, "") + XML;

				String defaultComparisonHTMLFileName = "CIMModelComparison_" + arguments[0].getName().replace(XMI, "")
						+ "_AND_" + arguments[1].getName().replace(XMI, "") + HTML;

				switch (arguments.length)
					{
					case 2:
						// Neither a target output directory or target HTML file was specified...

						modelComparisonXMLFile = new File(
								(arguments[0].getParentFile() != null ? arguments[0].getParentFile() : new File(".")),
								defaultComparisonXMLFileName);

						targetOutputHTMLFile = new File(
								(arguments[0].getParentFile() != null ? arguments[0].getParentFile() : new File(".")),
								defaultComparisonHTMLFileName);
						break;
					case 3:
						// A target output directory or target HTML file was specified...
						if (arguments[2].isDirectory()) {
							modelComparisonXMLFile = new File(arguments[2], defaultComparisonXMLFileName);
							targetOutputHTMLFile = new File(arguments[1], defaultComparisonHTMLFileName);
						} else {
							modelComparisonXMLFile = new File(
									(arguments[2].getParentFile() != null ? arguments[2].getParentFile()
											: new File(".")),
									arguments[2].getName().replace(HTML, "") + XML);
							targetOutputHTMLFile = new File(
									(arguments[2].getParentFile() != null ? arguments[2].getParentFile()
											: (arguments[0].getParentFile() != null ? arguments[0].getParentFile()
													: new File("."))),
									arguments[2].getName());
						}
						break;
					}

				DiffXMLGenerator.main(new String[] { arguments[0].getAbsolutePath(), arguments[1].getAbsolutePath(),
						modelComparisonXMLFile.getAbsolutePath() });
			} else {
				// Finally, we cover the case where we have a single XML EA comparison file to
				// be processed into an HTML comparison report output file...

				modelComparisonXMLFile = arguments[0];

				// Default name is based on the same name as the XML comparison file name...
				String defaultOutputHTMLFileName = arguments[0].getName().replace(XML, HTML);

				switch (arguments.length)
					{
					case 1:
						// Neither a target output directory or target HTML file was specified.
						// We specify that the file will reside in the same parent directory
						// that the XML file resides in...
						targetOutputHTMLFile = new File(
								(arguments[0].getParentFile() != null ? arguments[0].getParentFile() : new File(".")),
								defaultOutputHTMLFileName);
						break;
					case 2:
						// A target output directory or target HTML file was specified...
						if (arguments[1].isDirectory()) {
							// If a directory was specified we use it as the parent directory and then
							// use the default output HTML file name...
							targetOutputHTMLFile = new File(arguments[1], defaultOutputHTMLFileName);
						} else {
							targetOutputHTMLFile = new File(
									(arguments[1].getParentFile() != null ? arguments[1].getParentFile()
											: (arguments[0].getParentFile() != null ? arguments[0].getParentFile()
													: new File("."))),
									arguments[1].getName());
						}
						break;
					}
			}
		}

		return new File[] { modelComparisonXMLFile, targetOutputHTMLFile };
	}

	private static void printUsage() {
		System.err.println();
		System.err.println("There are two possible command line usages for the CIM Model Comparison Report utility:");
		System.err.println();
		System.err.println(
				"To generate an HTML report using the results file (*.xml) of an Enterprise Architect model comparison use the command line option.");
		System.err.println(
				"   Usage: java -jar cim-compare.jar <comparison-results-xml-file> [<output-directory-or-html-file>] [--package=<iec-package-name>] [--minimal]");
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
		System.err.println("          java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml --minimal");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml --package=IEC61970");
		System.err.println(
				"          java -jar cim-compare.jar CIM15v33_CIM16v26a_EA_Comparison_Report.xml --package=IEC61970 --minimal");
		System.err.println();
		System.err.println(
				"To generate a model comparison report directly from baseline and target models (XMI files) use the command line option.");
		System.err.println(
				"   Usage: java -jar cim-compare.jar <baseline-model-xmi-file> <target-model-xmi-file> [<output-directory-or-html-file>] [--package=<iec-package-name>] [--minimal]");
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
		System.err
				.println("          java -jar cim-compare.jar CIM15v33.xmi CIM16v26a.xmi --package=IEC62325 --minimal");
		System.err.println();
	}
}
