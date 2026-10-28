package org.cimug.compare.app;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.cimug.compare.SvgDiagrams;
import org.cimug.compare.app.CIMModelComparisonGenerator.DiagramImage;
import org.cimug.compare.uml1_3.DiagramElement;
import org.sparx.Repository;

/**
 * #141: diagram images drawn one at a time, after the comparison, for only the
 * diagrams the report can show (added, deleted or changed ones).
 * <p>
 * Both models' XMI is exported without images. While the comparison XML is built,
 * the comparison records which diagrams changed on each side; before the XML is
 * written, {@link #draw} reopens each project and writes those diagrams' images with
 * EA's {@code Project.PutDiagramImageToFile}, and {@link #boxes} gives the highlight
 * boxes (diagram positions) in each image's coordinates.
 * <p>
 * Every diagram is drawn as SVG, whose boxes give the diagram's offset
 * ({@link SvgDiagrams#shift}). For a raster format (PNG, JPG, GIF, BMP) the SVG is a
 * measuring stick, deleted afterwards: EA draws a single raster image exactly as the
 * SVG, the same size with every shape at the same pixel (EA 17, build 1721: 39 PNG/JPG
 * and SVG pairs of CIM 18v13 and 18v14 diagrams). Should an image be drawn at another
 * size, the boxes are scaled by its height.
 */
class PerDiagramImages {

	/** #139: how often EA may be reopened while one model's images are drawn. */
	static final int MAX_REOPEN = 3;

	private final File[] projects;
	private final File outputDir;
	private final DiagramImage format;
	/** Where the measuring-stick SVGs of a raster run go; deleted by {@link #finish}. */
	private final File measureRoot;
	/** Why images could not be drawn this way (not one SVG on a side), or null. */
	String unavailable;

	PerDiagramImages(File baselineProject, File destinationProject, File outputDir, DiagramImage format) {
		this.projects = new File[] { baselineProject, destinationProject };
		this.outputDir = outputDir;
		this.format = format;
		this.measureRoot = new File(outputDir, ".svg-measure");
	}

	DiagramImage format() {
		return format;
	}

	static String side(boolean baseline) {
		return baseline ? "Baseline" : "Destination";
	}

	File imagesDir(boolean baseline) {
		return new File(outputDir, "Images-" + (baseline ? "baseline" : "destination"));
	}

	/** The SVG of a diagram: the image itself, or the measuring stick of a raster image. */
	File svgFile(boolean baseline, String eaid) {
		return format == DiagramImage.SVG ? new File(imagesDir(baseline), eaid + ".svg")
				: new File(new File(measureRoot, baseline ? "baseline" : "destination"), eaid + ".svg");
	}

	File imageFile(boolean baseline, String eaid) {
		return new File(imagesDir(baseline), eaid + "." + format.ext());
	}

	/** Draws the images of the diagrams given for each side. */
	void draw(Set<String> baseline, Set<String> destination) {
		if (baseline.isEmpty() && destination.isEmpty())
			return;
		System.out.println("\nDrawing the " + (baseline.size() + destination.size()) + " image(s) of the diagrams that changed ("
				+ baseline.size() + " baseline, " + destination.size() + " destination) as " + format.name() + ".");
		drawSide(true, baseline);
		if (unavailable == null)
			drawSide(false, destination);
	}

	/** Draws one side's images with EA. Overridden in tests. */
	protected void drawSide(boolean baseline, Set<String> ids) {
		if (ids.isEmpty())
			return;
		String side = side(baseline);
		File project = projects[baseline ? 0 : 1];
		File images = imagesDir(baseline);
		images.mkdirs();
		if (format != DiagramImage.SVG)
			svgFile(baseline, "x").getParentFile().mkdirs();
		retries = 0;
		int svgs = 0, drawn = 0, reopened = 0;
		List<String> png = new ArrayList<String>(), none = new ArrayList<String>(), bare = new ArrayList<String>();
		Repository repository = open(project);
		try {
			for (String id : ids) {
				if (!alive(repository)) {
					if (reopened == MAX_REOPEN || (repository = reopen(repository, project, side)) == null) {
						none.add(id);
						continue;
					}
					reopened++;
				}
				File svg = svgFile(baseline, id);
				String reason = writeSvg(repository, id, svg);
				if (reason != null && reopened < MAX_REOPEN) {
					// #141: one more try in a fresh EA session (EA can fail on a diagram late in a
					// session and draw it once restarted)
					Repository fresh = reopen(repository, project, side);
					reopened++;
					if (fresh != null) {
						repository = fresh;
						reason = writeSvg(repository, id, svg);
					}
				}
				if (reason == null)
					svgs++;
				String name = alive(repository) ? diagramName(repository, id) : "";
				if (format == DiagramImage.SVG) {
					if (reason == null) {
						drawn++;
					} else if (alive(repository) && writeImage(repository, id, new File(images, id + ".png"))) {
						// #139: a diagram EA will not draw as SVG is shown as a PNG, without highlights
						png.add(name + id);
						System.out.println("\nWARNING:  " + side + " model: EA could not draw diagram " + name + id + " as SVG ("
								+ reason + "); it is shown as a PNG image, without highlights.");
					} else {
						none.add(name + id);
						System.out.println("\nWARNING:  " + side + " model: EA could not draw diagram " + name + id
								+ " as SVG or PNG (" + reason + "); the report shows no image of it.");
					}
				} else if (alive(repository) && writeImage(repository, id, imageFile(baseline, id))) {
					drawn++;
					if (reason != null) {
						bare.add(name + id);
						System.out.println("\nWARNING:  " + side + " model: EA could not draw diagram " + name + id + " as SVG ("
								+ reason + ") to place its highlights; its " + format.name() + " image is shown without them.");
					}
				} else {
					none.add(name + id);
					System.out.println("\nWARNING:  " + side + " model: EA could not draw diagram " + name + id + " as "
							+ format.name() + "; the report shows no image of it.");
				}
			}
		} finally {
			if (repository != null)
				CIMModelComparisonGenerator.closeQuietly(repository);
		}
		if (svgs == 0) {
			// not one SVG: this EA cannot draw diagrams one at a time after all
			unavailable = side + " model: EA did not write any SVG image";
			return;
		}
		System.out.println("\n" + side + " model: " + drawn + " diagram(s) drawn as " + format.name() + " images"
				+ (retries > 0 ? " (EA needed " + retries + " extra tr" + (retries == 1 ? "y" : "ies") + ")" : "")
				+ (png.isEmpty() ? "" : ", " + png.size() + " as PNG")
				+ (bare.isEmpty() ? "" : " (" + bare.size() + " without highlights)")
				+ (none.isEmpty() ? "" : ", " + none.size() + " without an image") + " to:  \n" + images.getAbsolutePath());
	}

	/**
	 * A diagram's highlight boxes (keyed "l,t,r,b" by its elements' diagram positions,
	 * as {@link SvgDiagrams#boxMap} keys them) in the coordinates of its image on one
	 * side; empty when there is no SVG to place them (the image is a PNG in place of an
	 * SVG, or missing) or it cannot be lined up.
	 */
	Map<String, String> boxes(boolean baseline, String eaid, List<DiagramElement> elements, String name) {
		if (elements == null)
			return new HashMap<String, String>();
		File svg = svgFile(baseline, eaid);
		if (!svg.isFile())
			return new HashMap<String, String>();
		String text;
		try {
			text = new String(Files.readAllBytes(svg.toPath()), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return new HashMap<String, String>();
		}
		int[] shift = SvgDiagrams.shift(text, elements);
		if (shift == null) {
			System.out.println("\nWARNING:  " + side(baseline) + " model: could not line up the highlights with the image of diagram '"
					+ name + "' (" + eaid + "); its highlights are left out.");
			return new HashMap<String, String>();
		}
		Map<String, String> map = SvgDiagrams.boxMap(elements, shift, text);
		if (format == DiagramImage.SVG)
			return map;
		File image = imageFile(baseline, eaid);
		int height = imageHeight(image);
		if (height <= 0)
			return new HashMap<String, String>();
		return SvgDiagrams.scale(map, rasterScale(SvgDiagrams.height(text), height));
	}

	/**
	 * The scale of a raster image drawn from an SVG {@code svgHeight} high: 1 when, as
	 * EA draws them, the image is the SVG's size; otherwise its height over the SVG's.
	 */
	static double rasterScale(double svgHeight, int imageHeight) {
		if (svgHeight <= 0 || imageHeight <= 0 || Math.abs(imageHeight - svgHeight) <= 1)
			return 1;
		return imageHeight / svgHeight;
	}

	/** An image's height in pixels, read from its header; -1 when it cannot be read. */
	static int imageHeight(File image) {
		if (!image.isFile())
			return -1;
		try (ImageInputStream in = ImageIO.createImageInputStream(image)) {
			if (in == null)
				return -1;
			Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
			if (!readers.hasNext())
				return -1;
			ImageReader reader = readers.next();
			try {
				reader.setInput(in);
				return reader.getHeight(0);
			} finally {
				reader.dispose();
			}
		} catch (IOException e) {
			return -1;
		}
	}

	/** Deletes the measuring-stick SVGs. */
	void finish() {
		CIMModelComparisonGenerator.deleteDirectory(measureRoot);
	}

	// ---- EA ---------------------------------------------------------------

	/** SVGs EA wrote only on a later try, for the summary line. */
	private int retries;

	private static Repository open(File project) {
		Repository r = new Repository();
		r.OpenFile(project.getAbsolutePath());
		return r;
	}

	/** Whether EA still answers (after a crash, GetProjectInterface returns null). */
	static boolean alive(Repository repository) {
		try {
			return repository != null && repository.GetProjectInterface() != null;
		} catch (Throwable e) {
			return false;
		}
	}

	/** Closes EA and opens the project again in a fresh session; null when it cannot be. */
	private static Repository reopen(Repository old, File project, String side) {
		System.out.println("\n" + side + " model: opening " + project.getName() + " again in a fresh Enterprise Architect session.");
		if (old != null)
			CIMModelComparisonGenerator.closeQuietly(old);
		System.gc();
		try {
			Repository r = open(project);
			return alive(r) ? r : null;
		} catch (Throwable e) {
			System.out.println("\nWARNING:  could not open " + project.getName() + " again (" + e.getMessage() + ").");
			return null;
		}
	}

	/** One diagram's image in the format of the file name (PNG, JPG, GIF, BMP); true when written. */
	private static boolean writeImage(Repository repository, String eaid, File file) {
		for (int attempt = 1; attempt <= 2; attempt++) {
			try {
				if (attempt > 1)
					Thread.sleep(1000);
				file.delete();
				repository.GetProjectInterface().PutDiagramImageToFile(
						attempt == 1 ? CIMModelComparisonGenerator.eaidToGuid(eaid) : eaid, file.getAbsolutePath(), 1);
				if (file.isFile() && file.length() > 8)
					return true;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			} catch (Throwable e) {
				// try again, then give up
			}
		}
		file.delete();
		return false;
	}

	/**
	 * Writes one diagram's SVG; null when it worked, else what EA said on each try.
	 * EA 17 sometimes fails on a diagram it draws fine a moment later ("Internal
	 * application error", or false with no file), so there are three tries: the GUID,
	 * the GUID in its XML form after a short pause, and loading the diagram and saving
	 * its image after a longer one.
	 */
	private String writeSvg(Repository repository, String eaid, File svg) {
		List<String> tries = new ArrayList<String>();
		svg.getParentFile().mkdirs();
		for (int attempt = 1; attempt <= 3; attempt++) {
			if (!alive(repository)) {
				tries.add("try " + attempt + ": EA stopped responding");
				break;
			}
			try {
				if (attempt > 1) {
					Thread.sleep(attempt == 2 ? 500 : 2000);
					retries++;
				}
				svg.delete();
				String said = "";
				boolean ok;
				if (attempt == 1) {
					ok = repository.GetProjectInterface().PutDiagramImageToFile(CIMModelComparisonGenerator.eaidToGuid(eaid),
							svg.getAbsolutePath(), 1);
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
				String text = new String(Files.readAllBytes(svg.toPath()), StandardCharsets.UTF_8);
				if (!SvgDiagrams.isSvg(text)) {
					tries.add("try " + attempt + ": not an SVG");
					continue;
				}
				Files.write(svg.toPath(), SvgDiagrams.withFontFallbacks(text).getBytes(StandardCharsets.UTF_8));
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

	/** EA's last error message, if it has one. */
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
			return "'" + repository.GetDiagramByGuid(CIMModelComparisonGenerator.eaidToGuid(eaid)).GetName() + "' ";
		} catch (Throwable e) {
			return "";
		}
	}
}
