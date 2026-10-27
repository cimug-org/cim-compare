package org.cimug.compare.app;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * EA writes diagram images to an {@code Images} folder, which is moved to
 * {@code Images-baseline} or {@code Images-destination} after each export. A
 * changed diagram has the same file name in both models, so the baseline's
 * images must be out of {@code Images} before the destination is exported
 * (issue #91).
 */
public class MoveImagesTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static void write(File dir, String name, int content) throws Exception {
		Files.write(new File(dir, name).toPath(), new byte[] { (byte) content });
	}

	private static byte[] read(File dir, String name) throws Exception {
		return Files.readAllBytes(new File(dir, name).toPath());
	}

	@Test
	public void baselineAndDestinationImagesStaySeparate() throws Exception {
		File images = tmp.newFolder("Images");
		File baseline = new File(tmp.getRoot(), "Images-baseline");
		File destination = new File(tmp.getRoot(), "Images-destination");

		write(images, "EAID_X.jpg", 1); // baseline export
		CIMModelComparisonGenerator.moveImages(images, baseline);
		assertFalse(images.exists());

		assertTrue(images.mkdir()); // destination export, same diagram
		write(images, "EAID_X.jpg", 2);
		CIMModelComparisonGenerator.moveImages(images, destination);

		assertArrayEquals(new byte[] { 1 }, read(baseline, "EAID_X.jpg"));
		assertArrayEquals(new byte[] { 2 }, read(destination, "EAID_X.jpg"));
	}

	/**
	 * When the rename cannot be done (here because the target already exists, as
	 * after an earlier run; on Windows also while EA holds the folder) the images
	 * are moved file by file, replacing older files of the same name.
	 */
	@Test
	public void imagesAreMovedWhenTheFolderCannotBeRenamed() throws Exception {
		File images = tmp.newFolder("Images");
		File baseline = tmp.newFolder("Images-baseline");
		write(baseline, "EAID_X.jpg", 9); // from an earlier run
		write(baseline, "EAID_OLD.jpg", 9);
		write(images, "EAID_X.jpg", 1);
		write(images, "EAID_Y.jpg", 1);

		CIMModelComparisonGenerator.moveImages(images, baseline);

		assertFalse(images.exists());
		assertArrayEquals(new byte[] { 1 }, read(baseline, "EAID_X.jpg"));
		assertArrayEquals(new byte[] { 1 }, read(baseline, "EAID_Y.jpg"));
		assertTrue("other files left alone", new File(baseline, "EAID_OLD.jpg").exists());
	}
}
