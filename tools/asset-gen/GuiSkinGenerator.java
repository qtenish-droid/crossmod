package tools;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates the smooth, resolution independent skin of the CrossMod shell.
 *
 * <p>The textures are drawn at four times their final size and are then
 * reduced by the game. This keeps every rounded corner and every one pixel
 * border anti aliased on every screen, without drawing hundreds of bands at
 * runtime. The variable width surfaces are horizontal slices: their corner
 * caps are fixed and their middle stretches, so the vertical gradient stays
 * exactly as it was painted.</p>
 *
 * <p>Run it with:</p>
 *
 * <pre>{@code java tools/asset-gen/GuiSkinGenerator.java <output-directory>}</pre>
 */
public final class GuiSkinGenerator {

    private static final int SCALE = 4;
    private static final int ICON = 128;
    private static final int HEADER_HEIGHT = 86;

    private GuiSkinGenerator() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: GuiSkinGenerator <output-directory>");
            System.exit(1);
        }

        Path output = Path.of(args[0]);
        Files.createDirectories(output);

        write(output.resolve("panel_base.png"), 128, 128, GuiSkinGenerator::drawPanelBase);
        write(output.resolve("panel_border.png"), 128, 128, GuiSkinGenerator::drawPanelBorder);
        write(output.resolve("header.png"), 256, HEADER_HEIGHT * SCALE, GuiSkinGenerator::drawHeader);
        write(output.resolve("card_on.png"), 256, 49 * SCALE, (g, w, h) -> drawCard(g, w, h, true));
        write(output.resolve("card_off.png"), 256, 49 * SCALE, (g, w, h) -> drawCard(g, w, h, false));
        write(output.resolve("nav_pill.png"), 256, 21 * SCALE, GuiSkinGenerator::drawNavPill);
        write(output.resolve("search_field.png"), 256, 29 * SCALE, GuiSkinGenerator::drawSearchField);
        write(output.resolve("filter_pill.png"), 256, 21 * SCALE, (g, w, h) -> drawFilterPill(g, w, h, false));
        write(output.resolve("filter_pill_selected.png"), 256, 21 * SCALE, (g, w, h) -> drawFilterPill(g, w, h, true));
        write(output.resolve("toggle_on.png"), 25 * SCALE, 13 * SCALE, (g, w, h) -> drawToggle(g, w, h, true));
        write(output.resolve("toggle_off.png"), 25 * SCALE, 13 * SCALE, (g, w, h) -> drawToggle(g, w, h, false));
        write(output.resolve("icon_tile.png"), 34 * SCALE, 34 * SCALE, GuiSkinGenerator::drawIconTile);

        writeIcon(output, "module_damage_tint.png", GuiSkinGenerator::drawDroplet);
        writeIcon(output, "module_clean_view.png", GuiSkinGenerator::drawEye);
        writeIcon(output, "module_crosshair_addons.png", GuiSkinGenerator::drawCrosshairAddons);
        writeIcon(output, "module_transparent_players.png", GuiSkinGenerator::drawPlayer);
        writeIcon(output, "module_pop_chams.png", GuiSkinGenerator::drawGhost);
        writeIcon(output, "module_nametags.png", GuiSkinGenerator::drawTag);
        writeIcon(output, "module_cobweb.png", GuiSkinGenerator::drawSnowflake);
        writeIcon(output, "module_block_overlay.png", GuiSkinGenerator::drawCube);
        writeIcon(output, "module_hide_armor.png", GuiSkinGenerator::drawShield);
        writeIcon(output, "module_crosshair.png", GuiSkinGenerator::drawCrosshair);
        writeIcon(output, "module_crossbow.png", GuiSkinGenerator::drawCrossbow);
        writeIcon(output, "module_renders.png", GuiSkinGenerator::drawHandSparkles);
        writeIcon(output, "module_hand.png", GuiSkinGenerator::drawHand);
        writeIcon(output, "module_tiertagger.png", GuiSkinGenerator::drawCrown);
        writeIcon(output, "module_hitboxes.png", GuiSkinGenerator::drawHitboxes);
        writeIcon(output, "module_keystrokes.png", GuiSkinGenerator::drawKeyboard);
        writeIcon(output, "module_visuals.png", GuiSkinGenerator::drawEye);
        writeIcon(output, "module_hud.png", GuiSkinGenerator::drawKeyboard);
        writeIcon(output, "module_pvp.png", GuiSkinGenerator::drawCrosshair);
        writeIcon(output, "module_player.png", GuiSkinGenerator::drawPlayer);
        writeIcon(output, "module_misc.png", GuiSkinGenerator::drawSparkle);
        writeIcon(output, "person.png", GuiSkinGenerator::drawPersonSolid);
        writeIcon(output, "external.png", GuiSkinGenerator::drawExternal);
        writeIcon(output, "edit_hud.png", GuiSkinGenerator::drawGrid);

        System.out.println("Wrote CrossMod skin textures to " + output.toAbsolutePath());
    }

    private static void write(Path target, int width, int height, Painter painter) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        applyQualityHints(graphics);
        painter.paint(graphics, width, height);
        graphics.dispose();
        ImageIO.write(image, "PNG", target.toFile());
    }

    private static void writeIcon(Path output, String name, IconPainter painter) throws IOException {
        write(output.resolve(name), ICON, ICON, (graphics, width, height) -> {
            graphics.setColor(Color.WHITE);
            graphics.setStroke(new BasicStroke(10.0F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            painter.paint(graphics);
        });
    }

    private static void applyQualityHints(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    interface Painter {
        void paint(Graphics2D graphics, int width, int height);
    }

    interface IconPainter {
        void paint(Graphics2D graphics);
    }

    /* ------------------------------------------------------------------ */
    /* Surfaces                                                            */
    /* ------------------------------------------------------------------ */

    private static void drawPanelBase(Graphics2D graphics, int width, int height) {
        graphics.setColor(new Color(32, 6, 10, 192));
        graphics.fill(new RoundRectangle2D.Double(0, 0, width, height, 48, 48));
    }

    private static void drawPanelBorder(Graphics2D graphics, int width, int height) {
        graphics.setPaint(new GradientPaint(0, 0, new Color(52, 53, 60, 150),
                0, height, new Color(27, 28, 33, 120)));
        graphics.setStroke(new BasicStroke(4.0F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(new RoundRectangle2D.Double(2, 2, width - 4, height - 4, 44, 44));
    }

    private static void drawHeader(Graphics2D graphics, int width, int height) {
        Path2D shape = new Path2D.Double();
        shape.moveTo(0, 0);
        shape.lineTo(width - 48, 0);
        shape.quadTo(width, 0, width, 48);
        shape.lineTo(width, height);
        shape.lineTo(0, height);
        shape.closePath();

        graphics.setPaint(new GradientPaint(0, 0, new Color(12, 13, 18, 252),
                0, height, new Color(9, 10, 14, 250)));
        graphics.fill(shape);

        graphics.setColor(new Color(39, 40, 46, 210));
        graphics.fill(new RoundRectangle2D.Double(0, height - 4, width, 4, 0, 0));

        graphics.setColor(new Color(255, 255, 255, 8));
        graphics.fill(new RoundRectangle2D.Double(48, 4, width - 96, 2, 0, 0));
    }

    private static void drawCard(Graphics2D graphics, int width, int height, boolean enabled) {
        double inset = 4;
        RoundRectangle2D body = new RoundRectangle2D.Double(inset, inset,
                width - inset * 2, height - inset * 2, 40, 40);

        graphics.setPaint(new GradientPaint(0, (float) inset, new Color(34, 35, 39, 244),
                0, (float) (height - inset), new Color(23, 24, 27, 244)));
        graphics.fill(body);

        graphics.setPaint(new GradientPaint(0, 0,
                enabled ? new Color(112, 69, 93, 220) : new Color(58, 57, 62, 190),
                0, height,
                enabled ? new Color(59, 41, 51, 190) : new Color(36, 35, 40, 170)));
        graphics.setStroke(new BasicStroke(4.0F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(body);

        graphics.setColor(new Color(255, 255, 255, enabled ? 13 : 9));
        graphics.fill(new RoundRectangle2D.Double(48, 6, width - 96, 2, 0, 0));
    }

    private static void drawNavPill(Graphics2D graphics, int width, int height) {
        RoundRectangle2D body = new RoundRectangle2D.Double(2, 2, width - 4, height - 4, 56, 56);

        graphics.setPaint(new GradientPaint(0, 2, new Color(85, 86, 90, 248),
                0, height - 2, new Color(73, 74, 78, 245)));
        graphics.fill(body);

        graphics.setPaint(new GradientPaint(0, 0, new Color(108, 109, 113, 180),
                0, height, new Color(60, 61, 65, 120)));
        graphics.setStroke(new BasicStroke(3.0F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(body);

        graphics.setColor(new Color(255, 255, 255, 12));
        graphics.fill(new RoundRectangle2D.Double(28, 6, width - 56, 2, 0, 0));
    }

    private static void drawSearchField(Graphics2D graphics, int width, int height) {
        RoundRectangle2D body = new RoundRectangle2D.Double(2, 2, width - 4, height - 4, 64, 64);

        graphics.setPaint(new GradientPaint(0, 2, new Color(8, 9, 13, 238),
                0, height - 2, new Color(6, 7, 10, 230)));
        graphics.fill(body);

        graphics.setPaint(new GradientPaint(0, 0, new Color(48, 49, 55, 155),
                0, height, new Color(28, 29, 34, 120)));
        graphics.setStroke(new BasicStroke(3.0F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(body);

        graphics.setColor(new Color(255, 255, 255, 6));
        graphics.fill(new RoundRectangle2D.Double(32, 6, width - 64, 2, 0, 0));
    }

    private static void drawFilterPill(Graphics2D graphics, int width, int height, boolean selected) {
        RoundRectangle2D body = new RoundRectangle2D.Double(2, 2, width - 4, height - 4, 80, 80);

        if (selected) {
            graphics.setPaint(new GradientPaint(0, 2, new Color(99, 99, 103, 250),
                    0, height - 2, new Color(81, 81, 86, 248)));
            graphics.fill(body);
            graphics.setPaint(new GradientPaint(0, 0, new Color(117, 117, 121, 130),
                    0, height, new Color(63, 63, 68, 120)));
        } else {
            graphics.setPaint(new GradientPaint(0, 2, new Color(23, 24, 28, 242),
                    0, height - 2, new Color(17, 18, 22, 238)));
            graphics.fill(body);
            graphics.setPaint(new GradientPaint(0, 0, new Color(82, 65, 75, 180),
                    0, height, new Color(44, 38, 43, 140)));
        }

        graphics.setStroke(new BasicStroke(3.0F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(body);

        graphics.setColor(new Color(255, 255, 255, selected ? 14 : 8));
        graphics.fill(new RoundRectangle2D.Double(40, 6, width - 80, 2, 0, 0));
    }

    private static void drawToggle(Graphics2D graphics, int width, int height, boolean active) {
        double knobRadius = height * 0.34;
        double centerY = height / 2.0;

        graphics.setColor(new Color(0, 0, 0, 55));
        graphics.fill(new RoundRectangle2D.Double(0, 2, width, height, height, height));

        RoundRectangle2D track = new RoundRectangle2D.Double(2, 2, width - 4, height - 4, height, height);
        graphics.setPaint(active
                ? new GradientPaint(0, 2, new Color(211, 90, 146, 255), 0, height - 2, new Color(187, 68, 121, 255))
                : new GradientPaint(0, 2, new Color(98, 95, 100, 252), 0, height - 2, new Color(74, 71, 76, 250)));
        graphics.fill(track);

        graphics.setPaint(active
                ? new GradientPaint(0, 0, new Color(238, 132, 177, 200), 0, height, new Color(158, 49, 101, 150))
                : new GradientPaint(0, 0, new Color(118, 114, 120, 190), 0, height, new Color(58, 56, 61, 150)));
        graphics.setStroke(new BasicStroke(3.0F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(track);

        double knobCenter = active ? width - 8 - knobRadius : 8 + knobRadius;
        graphics.setColor(new Color(0, 0, 0, 70));
        graphics.fill(new Ellipse2D.Double(knobCenter - knobRadius - 1, centerY - knobRadius + 2,
                knobRadius * 2 + 2, knobRadius * 2 + 2));
        graphics.setPaint(new GradientPaint(0, (float) (centerY - knobRadius), new Color(255, 255, 255, 255),
                0, (float) (centerY + knobRadius), new Color(226, 226, 230, 255)));
        graphics.fill(new Ellipse2D.Double(knobCenter - knobRadius, centerY - knobRadius,
                knobRadius * 2, knobRadius * 2));
    }

    private static void drawIconTile(Graphics2D graphics, int width, int height) {
        RoundRectangle2D body = new RoundRectangle2D.Double(2, 2, width - 4, height - 4, 40, 40);

        graphics.setPaint(new GradientPaint(0, 2, new Color(42, 42, 47, 248),
                0, height - 2, new Color(28, 29, 33, 244)));
        graphics.fill(body);

        graphics.setPaint(new GradientPaint(0, 0, new Color(60, 61, 66, 90),
                0, height, new Color(32, 33, 37, 50)));
        graphics.setStroke(new BasicStroke(3.0F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(body);

        graphics.setColor(new Color(255, 255, 255, 9));
        graphics.fill(new RoundRectangle2D.Double(24, 6, width - 48, 2, 0, 0));
    }

    /* ------------------------------------------------------------------ */
    /* Icons                                                               */
    /* ------------------------------------------------------------------ */

    private static void drawLogo(Graphics2D graphics) {
        Path2D outer = new Path2D.Double();
        outer.moveTo(48, 10);
        outer.lineTo(76, 10);
        outer.lineTo(114, 118);
        outer.lineTo(92, 118);
        outer.lineTo(82, 90);
        outer.lineTo(44, 90);
        outer.lineTo(34, 118);
        outer.lineTo(14, 118);
        outer.closePath();

        Area mark = new Area(outer);
        Path2D cutout = new Path2D.Double();
        cutout.moveTo(48, 78);
        cutout.lineTo(79, 78);
        cutout.lineTo(63, 36);
        cutout.closePath();
        mark.subtract(new Area(cutout));

        graphics.setColor(Color.WHITE);
        graphics.fill(mark);
    }

    private static void drawDroplet(Graphics2D graphics) {
        Path2D drop = new Path2D.Double();
        drop.moveTo(64, 14);
        drop.curveTo(64, 14, 34, 52, 34, 77);
        drop.curveTo(34, 94, 47, 108, 64, 108);
        drop.curveTo(81, 108, 94, 94, 94, 77);
        drop.curveTo(94, 52, 64, 14, 64, 14);
        drop.closePath();
        graphics.draw(drop);
    }

    private static void drawEye(Graphics2D graphics) {
        Path2D eye = new Path2D.Double();
        eye.moveTo(14, 64);
        eye.curveTo(32, 38, 48, 28, 64, 28);
        eye.curveTo(80, 28, 96, 38, 114, 64);
        eye.curveTo(96, 90, 80, 100, 64, 100);
        eye.curveTo(48, 100, 32, 90, 14, 64);
        eye.closePath();
        graphics.draw(eye);
        graphics.draw(new Ellipse2D.Double(50, 50, 28, 28));
    }

    private static void drawCrosshair(Graphics2D graphics) {
        graphics.draw(new Ellipse2D.Double(28, 28, 72, 72));
        graphics.draw(new Line2D.Double(64, 8, 64, 30));
        graphics.draw(new Line2D.Double(64, 98, 64, 120));
        graphics.draw(new Line2D.Double(8, 64, 30, 64));
        graphics.draw(new Line2D.Double(98, 64, 120, 64));
        graphics.fill(new Ellipse2D.Double(59, 59, 10, 10));
    }

    private static void drawCrosshairAddons(Graphics2D graphics) {
        graphics.draw(new Ellipse2D.Double(30, 30, 68, 68));
        graphics.draw(new Line2D.Double(64, 12, 64, 30));
        graphics.draw(new Line2D.Double(64, 98, 64, 116));
        graphics.draw(new Line2D.Double(12, 64, 30, 64));
        graphics.draw(new Line2D.Double(98, 64, 116, 64));
        graphics.fill(new Ellipse2D.Double(60, 60, 8, 8));
        graphics.draw(new RoundRectangle2D.Double(84, 16, 28, 24, 8, 8));
    }

    private static void drawPlayer(Graphics2D graphics) {
        graphics.draw(new Ellipse2D.Double(46, 18, 36, 36));
        Path2D body = new Path2D.Double();
        body.moveTo(24, 112);
        body.curveTo(26, 84, 42, 70, 64, 70);
        body.curveTo(86, 70, 102, 84, 104, 112);
        graphics.draw(body);
    }

    private static void drawPersonSolid(Graphics2D graphics) {
        graphics.fill(new Ellipse2D.Double(48, 20, 32, 32));
        Path2D body = new Path2D.Double();
        body.moveTo(22, 110);
        body.curveTo(25, 82, 42, 68, 64, 68);
        body.curveTo(86, 68, 103, 82, 106, 110);
        body.closePath();
        graphics.fill(body);
    }

    private static void drawGhost(Graphics2D graphics) {
        Path2D ghost = new Path2D.Double();
        ghost.moveTo(28, 110);
        ghost.lineTo(28, 60);
        ghost.curveTo(28, 34, 43, 18, 64, 18);
        ghost.curveTo(85, 18, 100, 34, 100, 60);
        ghost.lineTo(100, 110);
        ghost.lineTo(82, 96);
        ghost.lineTo(64, 110);
        ghost.lineTo(46, 96);
        ghost.closePath();
        graphics.draw(ghost);
        graphics.fill(new Ellipse2D.Double(48, 50, 8, 8));
        graphics.fill(new Ellipse2D.Double(72, 50, 8, 8));
    }

    private static void drawTag(Graphics2D graphics) {
        Path2D tag = new Path2D.Double();
        tag.moveTo(20, 26);
        tag.lineTo(57, 26);
        tag.lineTo(108, 77);
        tag.lineTo(75, 110);
        tag.lineTo(24, 59);
        tag.lineTo(24, 32);
        tag.closePath();
        graphics.draw(tag);
        graphics.draw(new Ellipse2D.Double(38, 40, 14, 14));
    }

    private static void drawSnowflake(Graphics2D graphics) {
        for (int i = 0; i < 3; i++) {
            double angle = Math.PI * i / 3.0;
            double dx = Math.cos(angle) * 52;
            double dy = Math.sin(angle) * 52;
            graphics.draw(new Line2D.Double(64 - dx, 64 - dy, 64 + dx, 64 + dy));

            for (int side = -1; side <= 1; side += 2) {
                double bx = 64 + dx * 0.62 * side;
                double by = 64 + dy * 0.62 * side;
                double branch = Math.PI / 5.0;
                graphics.draw(new Line2D.Double(bx, by,
                        bx + Math.cos(angle + branch) * 16 * side,
                        by + Math.sin(angle + branch) * 16 * side));
                graphics.draw(new Line2D.Double(bx, by,
                        bx + Math.cos(angle - branch) * 16 * side,
                        by + Math.sin(angle - branch) * 16 * side));
            }
        }
    }

    private static void drawCube(Graphics2D graphics) {
        Path2D cube = new Path2D.Double();
        cube.moveTo(64, 16);
        cube.lineTo(108, 40);
        cube.lineTo(108, 88);
        cube.lineTo(64, 112);
        cube.lineTo(20, 88);
        cube.lineTo(20, 40);
        cube.closePath();
        graphics.draw(cube);
        graphics.draw(new Line2D.Double(20, 40, 64, 64));
        graphics.draw(new Line2D.Double(64, 64, 108, 40));
        graphics.draw(new Line2D.Double(64, 64, 64, 112));
    }

    private static void drawShield(Graphics2D graphics) {
        Path2D shield = new Path2D.Double();
        shield.moveTo(64, 14);
        shield.lineTo(104, 30);
        shield.lineTo(104, 60);
        shield.curveTo(104, 86, 86, 104, 64, 114);
        shield.curveTo(42, 104, 24, 86, 24, 60);
        shield.lineTo(24, 30);
        shield.closePath();
        graphics.draw(shield);
        graphics.draw(new Line2D.Double(40, 42, 88, 92));
    }

    private static void drawCrossbow(Graphics2D graphics) {
        Path2D bow = new Path2D.Double();
        bow.moveTo(36, 20);
        bow.curveTo(70, 34, 94, 58, 94, 64);
        bow.curveTo(94, 70, 70, 94, 36, 108);
        graphics.draw(bow);
        graphics.draw(new Line2D.Double(28, 64, 108, 64));
        graphics.draw(new Line2D.Double(88, 44, 108, 64));
        graphics.draw(new Line2D.Double(88, 84, 108, 64));
        graphics.draw(new Line2D.Double(34, 28, 34, 100));
    }

    private static void drawHand(Graphics2D graphics) {
        Path2D hand = new Path2D.Double();
        hand.moveTo(38, 104);
        hand.lineTo(24, 82);
        hand.curveTo(18, 72, 28, 62, 38, 70);
        hand.lineTo(48, 80);
        hand.lineTo(48, 30);
        hand.curveTo(48, 20, 62, 20, 62, 30);
        hand.lineTo(62, 56);
        hand.lineTo(62, 22);
        hand.curveTo(62, 12, 76, 12, 76, 22);
        hand.lineTo(76, 54);
        hand.lineTo(76, 28);
        hand.curveTo(76, 18, 90, 18, 90, 28);
        hand.lineTo(90, 64);
        hand.lineTo(90, 46);
        hand.curveTo(90, 36, 104, 36, 104, 46);
        hand.lineTo(104, 82);
        hand.curveTo(104, 104, 88, 116, 68, 116);
        hand.curveTo(54, 116, 44, 112, 38, 104);
        hand.closePath();
        graphics.draw(hand);
    }

    private static void drawHandSparkles(Graphics2D graphics) {
        drawHand(graphics);
        graphics.setStroke(new BasicStroke(8.0F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(new Line2D.Double(98, 14, 98, 34));
        graphics.draw(new Line2D.Double(88, 24, 108, 24));
    }

    private static void drawCrown(Graphics2D graphics) {
        Path2D crown = new Path2D.Double();
        crown.moveTo(20, 92);
        crown.lineTo(26, 34);
        crown.lineTo(46, 56);
        crown.lineTo(64, 24);
        crown.lineTo(82, 56);
        crown.lineTo(102, 34);
        crown.lineTo(108, 92);
        crown.lineTo(92, 104);
        crown.lineTo(36, 104);
        crown.closePath();
        graphics.draw(crown);
        graphics.draw(new Line2D.Double(38, 78, 90, 78));
    }

    private static void drawHitboxes(Graphics2D graphics) {
        graphics.draw(new Line2D.Double(24, 44, 24, 24));
        graphics.draw(new Line2D.Double(24, 24, 44, 24));
        graphics.draw(new Line2D.Double(84, 24, 104, 24));
        graphics.draw(new Line2D.Double(104, 24, 104, 44));
        graphics.draw(new Line2D.Double(24, 84, 24, 104));
        graphics.draw(new Line2D.Double(24, 104, 44, 104));
        graphics.draw(new Line2D.Double(84, 104, 104, 104));
        graphics.draw(new Line2D.Double(104, 104, 104, 84));
        graphics.draw(new Ellipse2D.Double(54, 54, 20, 20));
    }

    private static void drawKeyboard(Graphics2D graphics) {
        graphics.draw(new RoundRectangle2D.Double(12, 36, 104, 56, 12, 12));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                double x = 26 + col * 18;
                double y = 50 + row * 12;
                graphics.fill(new RoundRectangle2D.Double(x, y, 8, 6, 3, 3));
            }
        }
    }

    private static void drawGrid(Graphics2D graphics) {
        graphics.draw(new RoundRectangle2D.Double(20, 20, 36, 36, 10, 10));
        graphics.draw(new RoundRectangle2D.Double(72, 20, 36, 36, 10, 10));
        graphics.draw(new RoundRectangle2D.Double(20, 72, 36, 36, 10, 10));
        graphics.draw(new RoundRectangle2D.Double(72, 72, 36, 36, 10, 10));
    }

    private static void drawExternal(Graphics2D graphics) {
        Path2D shape = new Path2D.Double();
        shape.moveTo(52, 24);
        shape.lineTo(108, 24);
        shape.lineTo(108, 80);
        graphics.draw(shape);
        graphics.draw(new Line2D.Double(108, 24, 60, 72));
        graphics.draw(new Line2D.Double(88, 24, 108, 24));
        Path2D bracket = new Path2D.Double();
        bracket.moveTo(78, 40);
        bracket.lineTo(24, 40);
        bracket.lineTo(24, 106);
        bracket.lineTo(90, 106);
        bracket.lineTo(90, 64);
        graphics.draw(bracket);
    }

    private static void drawSparkle(Graphics2D graphics) {
        Path2D sparkle = new Path2D.Double();
        sparkle.moveTo(64, 12);
        sparkle.quadTo(70, 50, 112, 64);
        sparkle.quadTo(70, 78, 64, 116);
        sparkle.quadTo(58, 78, 16, 64);
        sparkle.quadTo(58, 50, 64, 12);
        sparkle.closePath();
        graphics.draw(sparkle);
    }
}
