package org.cimug.compare.app;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;

import org.sparx.Collection;
import org.sparx.EnumXMIType;
import org.sparx.Package;
import org.sparx.Repository;

/**
 * EXPERIMENT, not for release: asks the installed Enterprise Architect to export one package's XMI
 * with diagram images once for each DiagramImage code, and reports what EA actually wrote, so we
 * can see which codes give SVG (or anything) on a given EA version.
 *
 * <pre>
 * java -Djava.library.path=C:\cim-compare\ea17-32 -cp cim-compare-2.2.0.jar org.cimug.compare.app.ImageExportProbe
 *      &lt;project.eap|.qea&gt; &lt;output-folder&gt; [&lt;package GUID or name&gt;] [&lt;codes, e.g. -1,0,1,2,3,4,5,6,7&gt;]
 * </pre>
 *
 * Pick a small package with a few diagrams: each code is a full export of it. Results are printed
 * and written to probe-results.txt in the output folder; each code's export is in code_N.
 */
public class ImageExportProbe {

	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Usage: ImageExportProbe <project.eap|.qea> <output-folder> [<package GUID or name>] [<codes, e.g. -1,0,1,2,3,4,5,6,7>]");
			System.exit(2);
		}
		File project = new File(args[0]).getAbsoluteFile();
		File out = new File(args[1]).getAbsoluteFile();
		String which = args.length > 2 ? args[2] : null;
		String codes = args.length > 3 ? args[3] : "-1,0,1,2,3,4,5,6,7";
		out.mkdirs();
		File report = new File(out, "probe-results.txt");
		try (PrintWriter log = new PrintWriter(new FileWriter(report, StandardCharsets.UTF_8))) {
			Repository repository = null;
			try {
				line(log, "cim-compare image export probe");
				line(log, "Java " + System.getProperty("java.version") + ", " + System.getProperty("sun.arch.data.model") + "-bit, java.library.path="
						+ System.getProperty("java.library.path"));
				repository = new Repository();
				repository.OpenFile(project.getPath());
				final Repository repo = repository;
				line(log, "EA library version (build): " + repository.GetLibraryVersion() + ", edition: " + safe(() -> String.valueOf(repository_edition(repo))));
				line(log, "Project: " + project);
				Package pkg = find(repository, which);
				if (pkg == null) {
					line(log, "ERROR: package not found: " + which);
					return;
				}
				int diagrams = countDiagrams(pkg);
				line(log, "Package: " + pkg.GetName() + " " + pkg.GetPackageGUID() + " (" + diagrams + " diagrams in it and below)");
				line(log, "");
				for (String c : codes.split(",")) {
					int code = Integer.parseInt(c.trim());
					File dir = new File(out, "code_" + code);
					dir.mkdirs();
					File xmi = new File(dir, "export.xmi");
					long t0 = System.currentTimeMillis();
					String result;
					try {
						result = repository.GetProjectInterface().ExportPackageXMI(pkg.GetPackageGUID(), EnumXMIType.xmiEA11, 2, code, 1, 0, xmi.getPath());
					} catch (Throwable e) {
						result = "EXCEPTION " + e;
					}
					long ms = System.currentTimeMillis() - t0;
					final Repository repo2 = repository;
					String lastError = safe(() -> repository_lastError(repo2));
					Map<String, Integer> byExt = new TreeMap<>();
					File images = new File(dir, "Images");
					File sample = null;
					File[] files = images.listFiles();
					if (files != null)
						for (File f : files) {
							String n = f.getName(), ext = n.contains(".") ? n.substring(n.lastIndexOf('.') + 1).toLowerCase() : "(none)";
							byExt.merge(ext, 1, Integer::sum);
							if (sample == null || ext.equals("svg")) sample = f;
						}
					line(log, "code " + code + ": " + ms + " ms, XMI " + (xmi.exists() ? xmi.length() + " bytes" : "NOT written")
							+ ", returned [" + result + "], GetLastError [" + lastError + "]");
					line(log, "   Images folder: " + (files == null ? "none" : byExt + ", e.g. " + (sample == null ? "-" : sample.getName() + " (" + sample.length() + " bytes)")));
					if (sample != null && sample.getName().toLowerCase().endsWith(".svg")) {
						String head = new String(Files.readAllBytes(sample.toPath()), StandardCharsets.UTF_8);
						int i = head.indexOf("<svg");
						line(log, "   SVG root: " + (i < 0 ? "(no <svg> element)" : head.substring(i, Math.min(head.length(), head.indexOf('>', i) + 1))));
					}
				}
			} catch (Throwable e) {
				line(log, "ERROR: " + e);
				e.printStackTrace(log);
			} finally {
				System.gc();
				if (repository != null) {
					repository.CloseFile();
					repository.Exit();
				}
			}
		}
		System.out.println("\nResults written to " + report);
	}

	private static Object repository_edition(Repository r) { return r.GetEAEditionEx(); }

	private static String repository_lastError(Repository r) { return r.GetProjectInterface().GetLastError(); }

	private interface S { String get() throws Exception; }

	private static String safe(S s) {
		try { return s.get(); } catch (Throwable e) { return "n/a (" + e + ")"; }
	}

	private static void line(PrintWriter log, String s) {
		System.out.println(s);
		log.println(s);
		log.flush();
	}

	/** The package with this GUID, else the first package with this name, else the first root node. */
	private static Package find(Repository r, String which) {
		if (which == null || which.isEmpty()) return r.GetModels().GetAt((short) 0);
		if (which.startsWith("{")) {
			try { return r.GetPackageByGuid(which); } catch (Throwable e) { /* try by name */ }
		}
		Collection<Package> models = r.GetModels();
		for (short i = 0; i < models.GetCount(); i++) {
			Package p = byName(models.GetAt(i), which);
			if (p != null) return p;
		}
		return null;
	}

	private static Package byName(Package p, String name) {
		if (name.equals(p.GetName())) return p;
		Collection<Package> kids = p.GetPackages();
		for (short i = 0; i < kids.GetCount(); i++) {
			Package f = byName(kids.GetAt(i), name);
			if (f != null) return f;
		}
		return null;
	}

	private static int countDiagrams(Package p) {
		int n = p.GetDiagrams().GetCount();
		Collection<Package> kids = p.GetPackages();
		for (short i = 0; i < kids.GetCount(); i++) n += countDiagrams(kids.GetAt(i));
		return n;
	}
}
