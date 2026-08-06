/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.Objects;

import sun.java2d.SunGraphics2D;

/**
 * @test
 * @bug 8004859 6513150
 * @summary getClipBounds/getClip should return equivalent bounds.
 * @author Sergey Bylokhov
 * @modules java.desktop/sun.java2d
 *          java.desktop/sun.java2d.pipe
 */
public final class TestSetGetClip {

    private static final Rectangle[] CLIPS = {
        new Rectangle(0, 0, -1, -1),
        new Rectangle(100, 100, -100, -100),
        null
    };

    private static boolean status = true;

    public static void main(final String[] args) {
        final BufferedImage bi = new BufferedImage(300, 300,
                                                   BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = bi.createGraphics();
        test(g);
        g.translate(2.0, 2.0);
        test(g);
        g.translate(-4.0, -4.0);
        test(g);
        g.scale(2.0, 2.0);
        test(g);
        g.scale(-4.0, -4.0);
        test(g);
        g.rotate(Math.toRadians(90));
        test(g);
        g.rotate(Math.toRadians(90));
        test(g);
        g.rotate(Math.toRadians(90));
        test(g);
        g.rotate(Math.toRadians(90));
        test(g);
        g.scale(0, 1); // non-invertible
        test(g);
        g.dispose();
        if (!status) {
            throw new RuntimeException("Test failed");
        }
    }

    private static void test(final Graphics2D g) {
        for (final Rectangle clip : CLIPS) {
            test(g, clip);
        }
    }

    private static void test(final Graphics2D g, final Rectangle clip) {

        g.setClip(clip);

        // test getClip()
        Shape expected = clip;
        Shape getClip = g.getClip();
        if (!Objects.equals(expected, getClip)) {
            err("Expected clip: " + expected);
            err("Actual clip: " + getClip);
            err("bounds=" + (getClip != null ? getClip.getBounds2D() : null));
            err("bounds=" + (getClip != null ? getClip.getBounds() : null));
            status = false;
        }

        // test getClipBounds()
        Rectangle bounds = g.getClipBounds();
        if (!Objects.equals(expected, bounds)) {
            err("Expected getClipBounds(): " + expected);
            err("Actual getClipBounds(): " + bounds);
            status = false;
        }

        // test getClipBounds(Rectangle); same expectations as getClipBounds(),
        // with added condition that if null Rectangle is passed in, NPE throws
        boolean npe = false;
        try {
            g.getClipBounds(bounds);
        } catch (NullPointerException e) {
            npe = true;
        }
        if (bounds == null && !npe) {
            err("Expected NullPointerException");
            err("Actual getClipBounds(r): " + bounds);
            status = false;
        }
        if (bounds != null && npe) {
            err("Expected getClipBounds(r): " + expected);
            err("Actual NullPointerException");
            status = false;
        }
        if (bounds != null && !Objects.equals(expected, bounds)) {
            err("Expected getClipBounds(r): " + expected);
            err("Actual getClipBounds(r): " + bounds);
            status = false;
        }

        // test clipRegion
        if (clip != null &&
            !clip.getBounds2D().isEmpty() &&
            ((SunGraphics2D) g).clipRegion.isEmpty()) {
            err("clipRegion should not be empty");
            status = false;
        }
    }

    private static void err(String s) {
        System.err.println(s);
    }
}
