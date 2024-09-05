/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/*
 * @test
 */
public class RotatedScaledFontTest {

    public static void main(String[] args) throws Exception {
        test(0);
        test(1);
        test(2);
        test(3);
        test(4);
    }

    private static void test(int quadrants) throws Exception {

        Font base = new Font("SansSerif", Font.PLAIN, 10);
        BufferedImage image = new BufferedImage(600, 600, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = image.createGraphics();

        try {
            for (int scale = 1; scale <= 100; scale++) {
                AffineTransform at = AffineTransform.getQuadrantRotateInstance(quadrants);
                at.scale(scale, scale);
                Font font = base.deriveFont(at);
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
                g2d.setColor(Color.BLACK);
                g2d.setFont(font);
                g2d.drawString("TEST", 300, 300);
                if (!hasBlackPixels(image)) {
                    throw new RuntimeException("Text missing: scale=" + scale + ", quadrants=" + quadrants);
                }
            }
        } finally {
            g2d.dispose();
        }
    }

    private static boolean hasBlackPixels(BufferedImage image) {
        DataBufferByte buffer = (DataBufferByte) image.getRaster().getDataBuffer();
        byte[] pixels = buffer.getData();
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] == 0) {
                return true;
            }
        }
        return false;
    }
}
