/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/* @test
 * @summary Confirm that glyph vectors with rotated fonts render correctly.
 * @bug 8148334
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

public class GlyphVectorRotatedFont {

    public static void main(String[] args) {

        FontRenderContext frc = new FontRenderContext(null, true, true);
        Font font1 = new Font(Font.DIALOG, Font.PLAIN, 40);
        Font font2 = font1.deriveFont(AffineTransform.getRotateInstance(0.3));
        String text = "THIS IS A TEST";

        GlyphVector gv1 = font1.createGlyphVector(frc, text);
        GlyphVector gv2 = font2.createGlyphVector(frc, text);
        GlyphVector gv3 = font2.createGlyphVector(frc, text);
        adjustGlyphPositions(gv3);

        Rectangle bounds1 = gv1.getVisualBounds().getBounds();
        Rectangle bounds2 = gv2.getVisualBounds().getBounds();
        Rectangle bounds3 = gv3.getVisualBounds().getBounds();

        assertTaller(bounds2, bounds1);
        assertTaller(bounds3, bounds1);

        int w = 500;
        int h = 500;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, w, h);
        g2d.setColor(Color.BLACK);
        g2d.drawGlyphVector(gv1, w / 2f, h / 2f);
        Rectangle pixelBounds1 = findTextBoundingBox(image);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, w, h);
        g2d.setColor(Color.BLACK);
        g2d.drawGlyphVector(gv2, w / 2f, h / 2f);
        Rectangle pixelBounds2 = findTextBoundingBox(image);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, w, h);
        g2d.setColor(Color.BLACK);
        g2d.drawGlyphVector(gv3, w / 2f, h / 2f);
        Rectangle pixelBounds3 = findTextBoundingBox(image);

        g2d.dispose();

        assertTaller(pixelBounds2, pixelBounds1);
        assertTaller(pixelBounds3, pixelBounds1);
    }

    private static void assertTaller(Rectangle rotated, Rectangle unrotated) {
        if (rotated.getHeight() < unrotated.getHeight() * 3) {
            throw new RuntimeException("Expected bounds " + rotated + " of rotated text " +
                    "to be much taller than bounds " + unrotated + " of unrotated text.");
        }
    }

    private static void adjustGlyphPositions(GlyphVector gv) {
        int count = gv.getNumGlyphs();
        for (int i = 0; i < count; i++) {
            int delta = (i % 2 == 0 ? 5 : -5);
            Point2D pt = gv.getGlyphPosition(i);
            pt.setLocation(pt.getX(), pt.getY() + delta);
            gv.setGlyphPosition(i, pt);
        }
    }

    private static Rectangle findTextBoundingBox(BufferedImage image) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int width = image.getWidth();
        int height = image.getHeight();
        int[] rowPixels = new int[width];
        for (int y = 0; y < height; y++) {
            image.getRGB(0, y, width, 1, rowPixels, 0, width);
            for (int x = 0; x < width; x++) {
                boolean white = (rowPixels[x] == -1);
                if (!white) {
                    if (x < minX) {
                        minX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                }
            }
        }
        if (minX != Integer.MAX_VALUE) {
            return new Rectangle(minX, minY, maxX - minX, maxY - minY);
        } else {
            return new Rectangle();
        }
    }
}
