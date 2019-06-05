/* 
 * Copyright 2014 Igor Maznitsa (http://www.igormaznitsa.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.prol.easygui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Misc auxiliary methods for UI operations.
 *
 * @author Igor Maznitsa (igor.maznitsa@igormaznitsa.com)
 */
public final class UiUtils {

    private UiUtils() {

    }

    public static void closeQuetly(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ex) {
            }
        }
    }

    public static void doInSwingThread(final Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    public static Pattern makePattern(final String str) {
        if (str.isEmpty()) {
            return null;
        }

        final StringBuilder buffer = new StringBuilder(str.length() << 1);

        for (final char c : str.toCharArray()) {
            if (Character.isLetter(c) || Character.isDigit(c)) {
                buffer.append(c);
            } else {
                if (Character.isWhitespace(c)) {
                    buffer.append("\\s");
                } else {
                    switch (c) {
                        case '*':
                            buffer.append(".*");
                            break;
                        case '?':
                            buffer.append(".");
                            break;
                        default: {
                            final String ucode = Integer.toHexString(c).toUpperCase(Locale.ENGLISH);
                            buffer.append("\\u").append("0000".substring(4 - ucode.length())).append(ucode);
                        }
                        break;
                    }
                }
            }
        }
        return Pattern.compile(buffer.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    public static boolean browseURL(final URL url) {
        boolean result = false;
        if (Desktop.isDesktopSupported()) {
            final Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(url.toURI());
                    result = true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else if (desktop.isSupported(Desktop.Action.OPEN)) {
                try {
                    desktop.open(new File(url.toURI()));
                    result = true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return result;
    }

    public static ImageIcon loadIcon(final String name) {
        try {
            final Image img;
            try (InputStream inStream = UiUtils.class.getClassLoader().getResourceAsStream("com/igormaznitsa/prol/easygui/icons/" + name + ".png")) {
                img = ImageIO.read(inStream);
            }
            return new ImageIcon(img);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ImageIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB));
        }
    }

    public static void assertSwingThread() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new Error("Must e called in Swing Dispatch Event Thread");

        }
    }
}
