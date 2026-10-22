package tools;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates every static texture shipped with CrossMod.
 *
 * <p>The textures are produced procedurally so that they stay perfectly
 * consistent, anti aliased and reviewable - there is no binary blob in the
 * repository that nobody can regenerate. Run it with:</p>
 *
 * <pre>{@code java tools/asset-gen/IconGenerator.java <output-directory>}</pre>
 */
public final class IconGenerator {

	private static final int SIZE = 128;

	private IconGenerator() {
	}

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.println("Usage: IconGenerator <output-directory>");
			System.exit(1);
		}

		Path output = Path.of(args[0]);
		Files.createDirectories(output);

		write(output.resolve("gear.png"), IconGenerator::drawGear);
		write(output.resolve("discord.png"), IconGenerator::drawDiscord);
		write(output.resolve("search.png"), IconGenerator::drawSearch);
		write(output.resolve("close.png"), IconGenerator::drawClose);
		write(output.resolve("check.png"), IconGenerator::drawCheck);
		write(output.resolve("chevron_left.png"), IconGenerator::drawChevronLeft);
		write(output.resolve("chevron_right.png"), IconGenerator::drawChevronRight);
		write(output.resolve("back.png"), IconGenerator::drawBack);
		write(output.resolve("crossmod_icon.png"), IconGenerator::drawModIcon);

		System.out.println("Wrote textures to " + output.toAbsolutePath());
	}

	private static void write(Path target, IconDrawer drawer) throws IOException {
		BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		applyQualityHints(graphics);
		drawer.draw(graphics, SIZE);
		graphics.dispose();
		ImageIO.write(image, "PNG", target.toFile());
	}

	private static void applyQualityHints(Graphics2D graphics) {
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	}

	private interface IconDrawer {
		void draw(Graphics2D graphics, int size);
	}

	/* ------------------------------------------------------------------ */
	/* Icons                                                               */
	/* ------------------------------------------------------------------ */

	/**
	 * Draws a classic cog wheel: a toothed ring with a hollow centre.
	 */
	private static void drawGear(Graphics2D graphics, int size) {
		double center = size / 2.0;
		double outer = size * 0.44;
		double inner = size * 0.30;
		double hole = size * 0.13;

		Path2D gear = new Path2D.Double();

		int teeth = 8;
		double toothWidth = Math.PI * 2 / teeth * 0.42;

		for (int i = 0; i < teeth; i++) {
			double angle = i * Math.PI * 2 / teeth;

			Path2D.Double tooth = new Path2D.Double();
			tooth.append(new RoundRectangle2D.Double(center - outer * 0.17, center - outer, outer * 0.34, outer * 0.42, 6, 6), false);

			AffineTransform transform = AffineTransform.getRotateInstance(angle, center, center);
			gear.append(transform.createTransformedShape(tooth), false);
		}

		gear.append(new Ellipse2D.Double(center - inner, center - inner, inner * 2, inner * 2), false);

		graphics.setColor(Color.WHITE);
		graphics.fill(gear);

		graphics.setComposite(java.awt.AlphaComposite.Clear);
		graphics.fill(new Ellipse2D.Double(center - hole, center - hole, hole * 2, hole * 2));
		graphics.setComposite(java.awt.AlphaComposite.SrcOver);
	}

	/**
	 * Draws the Discord mark: the rounded "face" silhouette with two eyes.
	 */
	private static void drawDiscord(Graphics2D graphics, int size) {
		double s = size / 100.0;

		Path2D shape = new Path2D.Double();
		shape.moveTo(82.5 * s, 12.9 * s);
		shape.curveTo(79.4 * s, 5.7 * s, 70.6 * s, 3.1 * s, 63.8 * s, 7.3 * s);
		shape.lineTo(55.0 * s, 12.7 * s);
		shape.curveTo(53.4 * s, 13.7 * s, 51.6 * s, 14.2 * s, 49.8 * s, 14.2 * s);
		shape.lineTo(49.5 * s, 14.2 * s);
		shape.curveTo(47.7 * s, 14.2 * s, 45.9 * s, 13.6 * s, 44.3 * s, 12.6 * s);
		shape.lineTo(35.5 * s, 7.2 * s);
		shape.curveTo(28.7 * s, 3.0 * s, 19.9 * s, 5.6 * s, 16.8 * s, 12.8 * s);
		shape.curveTo(5.9 * s, 33.6 * s, 4.4 * s, 46.2 * s, 6.6 * s, 58.6 * s);
		shape.curveTo(7.8 * s, 65.4 * s, 12.4 * s, 71.2 * s, 18.4 * s, 74.3 * s);
		shape.lineTo(26.1 * s, 78.4 * s);
		shape.curveTo(29.5 * s, 80.2 * s, 33.6 * s, 79.8 * s, 36.9 * s, 77.4 * s);
		shape.lineTo(42.8 * s, 72.5 * s);
		shape.curveTo(44.3 * s, 71.3 * s, 46.6 * s, 71.3 * s, 48.1 * s, 72.5 * s);
		shape.lineTo(54.0 * s, 77.4 * s);
		shape.curveTo(57.3 * s, 79.8 * s, 61.4 * s, 80.2 * s, 64.8 * s, 78.4 * s);
		shape.lineTo(72.5 * s, 74.3 * s);
		shape.curveTo(78.5 * s, 71.2 * s, 83.1 * s, 65.4 * s, 84.3 * s, 58.6 * s);
		shape.curveTo(86.5 * s, 46.2 * s, 85.0 * s, 33.6 * s, 82.5 * s, 12.9 * s);
		shape.closePath();

		Path2D eyes = new Path2D.Double();
		eyes.append(new Ellipse2D.Double(31.1 * s, 46.2 * s, 21.2 * s, 22.2 * s), false);
		eyes.append(new Ellipse2D.Double(61.8 * s, 46.2 * s, 21.2 * s, 22.2 * s), false);

		// The eyes are cut out of the silhouette.
		java.awt.geom.Area area = new java.awt.geom.Area(shape);
		area.subtract(new java.awt.geom.Area(eyes));

		graphics.setColor(Color.WHITE);
		graphics.fill(area);
	}

	/**
	 * Draws a magnifying glass.
	 */
	private static void drawSearch(Graphics2D graphics, int size) {
		double s = size / 128.0;

		graphics.setColor(Color.WHITE);
		graphics.setStroke(new BasicStroke((float) (10 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.draw(new Ellipse2D.Double(22 * s, 20 * s, 58 * s, 58 * s));

		graphics.setStroke(new BasicStroke((float) (12 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.draw(new java.awt.geom.Line2D.Double(72 * s, 74 * s, 104 * s, 106 * s));
	}

	/**
	 * Draws a cross.
	 */
	private static void drawClose(Graphics2D graphics, int size) {
		double s = size / 128.0;

		graphics.setColor(Color.WHITE);
		graphics.setStroke(new BasicStroke((float) (11 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.draw(new java.awt.geom.Line2D.Double(36 * s, 36 * s, 92 * s, 92 * s));
		graphics.draw(new java.awt.geom.Line2D.Double(92 * s, 36 * s, 36 * s, 92 * s));
	}

	/**
	 * Draws a check mark.
	 */
	private static void drawCheck(Graphics2D graphics, int size) {
		double s = size / 128.0;

		graphics.setColor(Color.WHITE);
		graphics.setStroke(new BasicStroke((float) (12 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.draw(new java.awt.geom.Line2D.Double(30 * s, 66 * s, 54 * s, 90 * s));
		graphics.draw(new java.awt.geom.Line2D.Double(54 * s, 90 * s, 98 * s, 40 * s));
	}

	/**
	 * Draws a chevron pointing left.
	 */
	private static void drawChevronLeft(Graphics2D graphics, int size) {
		double s = size / 128.0;

		graphics.setColor(Color.WHITE);
		graphics.setStroke(new BasicStroke((float) (11 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.draw(new java.awt.geom.Line2D.Double(80 * s, 26 * s, 44 * s, 64 * s));
		graphics.draw(new java.awt.geom.Line2D.Double(44 * s, 64 * s, 80 * s, 102 * s));
	}

	/**
	 * Draws a chevron pointing right.
	 */
	private static void drawChevronRight(Graphics2D graphics, int size) {
		double s = size / 128.0;

		graphics.setColor(Color.WHITE);
		graphics.setStroke(new BasicStroke((float) (11 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.draw(new java.awt.geom.Line2D.Double(48 * s, 26 * s, 84 * s, 64 * s));
		graphics.draw(new java.awt.geom.Line2D.Double(84 * s, 64 * s, 48 * s, 102 * s));
	}

	/**
	 * Draws a back arrow.
	 */
	private static void drawBack(Graphics2D graphics, int size) {
		double s = size / 128.0;

		graphics.setColor(Color.WHITE);
		graphics.setStroke(new BasicStroke((float) (11 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.draw(new java.awt.geom.Line2D.Double(102 * s, 64 * s, 32 * s, 64 * s));
		graphics.draw(new java.awt.geom.Line2D.Double(60 * s, 36 * s, 32 * s, 64 * s));
		graphics.draw(new java.awt.geom.Line2D.Double(32 * s, 64 * s, 60 * s, 92 * s));
	}

	/**
	 * Draws the mod icon: a dark glass tile with a glowing cross.
	 */
	private static void drawModIcon(Graphics2D graphics, int size) {
		double s = size / 128.0;

		graphics.setPaint(new java.awt.GradientPaint(0, 0, new Color(24, 30, 44), 0, size, new Color(10, 13, 20)));
		graphics.fill(new RoundRectangle2D.Double(4 * s, 4 * s, 120 * s, 120 * s, 26 * s, 26 * s));

		graphics.setPaint(new java.awt.GradientPaint(0, 0, new Color(120, 224, 255), size, size, new Color(58, 130, 246)));
		graphics.setStroke(new BasicStroke((float) (13 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.draw(new java.awt.geom.Line2D.Double(44 * s, 44 * s, 84 * s, 84 * s));
		graphics.draw(new java.awt.geom.Line2D.Double(84 * s, 44 * s, 44 * s, 84 * s));

		graphics.setColor(new Color(255, 255, 255, 220));
		graphics.fill(new Ellipse2D.Double(56 * s, 56 * s, 16 * s, 16 * s));
	}
}
