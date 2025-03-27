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

/**
 * @test
 * @bug 7156751
 */

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Function;

public class GlyphVectorMetricsTest {

    public static void main(String[] args) {

        List< Font > fonts = List.of(
            new Font(Font.SERIF, Font.PLAIN, 80),
            new Font(Font.SANS_SERIF, Font.PLAIN, 80),
            new Font(Font.MONOSPACED, Font.PLAIN, 80)
        );

        AffineTransform identity = new AffineTransform();
        AffineTransform scales2x = AffineTransform.getScaleInstance(2, 2);
        AffineTransform move100x = AffineTransform.getTranslateInstance(100, 0);
        AffineTransform move100y = AffineTransform.getTranslateInstance(0, 100);
        AffineTransform rotate45 = AffineTransform.getRotateInstance(Math.PI / 4);
        AffineTransform rotate90 = AffineTransform.getQuadrantRotateInstance(1);

        List< AffineTransform > txs = List.of(
            identity,
            scales2x,
            move100x,
            move100y,
            rotate45,
            rotate90,
            concatenate(scales2x, move100x),
            concatenate(move100x, scales2x),
            concatenate(rotate45, move100y, scales2x)
        );

        for (Font font : fonts) {
            for (boolean aa : List.of(true, false)) {
                for (boolean fm : List.of(true, false)) {
                    for (AffineTransform tx : txs) {
                        test(font, aa, fm, tx, identity);
                        test(font, aa, fm, identity, tx);
                        test(font, aa, fm, tx, tx);
                    }
                }
            }
        }
    }

    private static void test(Font font, boolean aa, boolean fm,
                             AffineTransform deviceTx, AffineTransform fontTx) {

        // EXPECTATIONS
        // we consider the information from a "vanilla" GV to be our "source of truth"

        String text = "Ab1";
        FontRenderContext frc = new FontRenderContext(null, aa, fm);
        GlyphVector gv1 = font.createGlyphVector(frc, text);

        // ACTUAL
        // get the glyph information to be checked, using a GV configured per method params

        Font font2 = font.deriveFont(fontTx);
        FontRenderContext frc2 = new FontRenderContext(deviceTx, aa, fm);
        GlyphVector gv2 = font2.createGlyphVector(frc2, text);

        // COMPARE
        // we transform the expectations as needed, and ensure they match the actual values

        int len = text.length();
        AffineTransform fontTxNoTrans = removeTranslation(fontTx);
        String id = "[font=" + font.getName() + " deviceTx=" + deviceTx + " fontTx=" + fontTx +
                    " aa=" + aa + " fm=" + fm + "]";

        compareEach("glyph logical bounds " + id, len,
            i -> transform(gv1.getGlyphLogicalBounds(i), fontTx),
            i -> gv2.getGlyphLogicalBounds(i));

        compareEach("glyph outline 1 " + id, len,
            i -> transform(gv1.getGlyphOutline(i), fontTx),
            i -> gv2.getGlyphOutline(i));

        compareEach("glyph outline 2 " + id, len,
            i -> transform(gv1.getGlyphOutline(i, 0, 0), fontTx),
            i -> gv2.getGlyphOutline(i, 0, 0));

        compareEach("glyph position " + id, len,
            i -> transform(toShape(gv1.getGlyphPosition(i)), fontTx),
            i -> toShape(gv2.getGlyphPosition(i)));

        compareEach("glyph pixel bounds " + id, len,
            i -> transform(gv1.getGlyphOutline(i), fontTx, deviceTx),
            i -> gv2.getGlyphPixelBounds(i, null, 0, 0));

        compareEach("glyph visual bounds " + id, len,
            i -> transform(gv1.getGlyphOutline(i), fontTx),
            i -> gv2.getGlyphVisualBounds(i));

        compareEach("glyph metrics advance x/y " + id, len,
            i -> transform(toShape(
                    gv1.getGlyphMetrics(i).getAdvanceX(),
                    gv1.getGlyphMetrics(i).getAdvanceY()), fontTxNoTrans),
            i -> toShape(
                    gv2.getGlyphMetrics(i).getAdvanceX(),
                    gv2.getGlyphMetrics(i).getAdvanceY()));
    }

    private static void compareEach(String desc, int glyphs,
        Function< Integer, Shape > f1, Function< Integer, Shape > f2) {
        for (int i = 0; i < glyphs; i++) {
            Shape s1 = f1.apply(i);
            Shape s2 = f2.apply(i);
            Rectangle expected = s1.getBounds();
            Rectangle actual = s2.getBounds();
            int allowance = 2;
            String msg = desc + " (glyph " + i + ")";
            assertRoughlyEqual(expected, actual, allowance, msg);
        }
    }

    private static Shape transform(Shape s, AffineTransform... txs) {
        AffineTransform tx = new AffineTransform();
        for (int i = 0; i < txs.length; i++) {
            tx.concatenate(txs[i]);
        }
        return tx.createTransformedShape(s);
    }

    private static AffineTransform concatenate(AffineTransform... txs) {
        AffineTransform tx = txs[0];
        for (int i = 1; i < txs.length; i++) {
            tx.concatenate(txs[i]);
        }
        return tx;
    }

    private static void assertRoughlyEqual(Rectangle expected, Rectangle actual,
        int delta, String msg) {
        int deltaX = Math.abs(expected.x - actual.x);
        int deltaY = Math.abs(expected.y - actual.y);
        int deltaW = Math.abs(expected.width - actual.width);
        int deltaH = Math.abs(expected.height - actual.height);
        if (deltaX > delta || deltaY > delta || deltaW > delta || deltaH > delta) {
            throw new RuntimeException(msg + ": " + expected + " != " + actual);
        }
    }

    private static Rectangle toShape(Point2D pt) {
        return new Rectangle((int) pt.getX(), (int) pt.getY(), 0, 0);
    }

    private static Rectangle toShape(float x, float y) {
        return new Rectangle(0, 0, (int) x, (int) y);
    }

    private static AffineTransform removeTranslation(AffineTransform t) {
        return new AffineTransform(t.getScaleX(), t.getShearY(),
                                   t.getShearX(), t.getScaleY(),
                                   0, 0);
    }
}
