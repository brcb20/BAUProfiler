/*
 * This file is part of Tornado: A heterogeneous programming framework:
 * https://github.com/beehive-lab/tornado
 *
 * Copyright (c) 2013-2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 * Authors: Benjamin Bell
 *
 */
package uk.ac.manchester.bauprofiler.json.core.utils;

public class JsonUtils {
    public static String prettyPrint(String uglyJson, int spacing) {
        boolean inside = false;
        String indent = "";
        String space = new String(new char[spacing]).replace("\0", " ");
        StringBuilder buffer = new StringBuilder(uglyJson.length() + 20);

        for (int i = 0; i < uglyJson.length(); i++) {
            char ch = uglyJson.charAt(i);
            if (ch == '"') {
                inside = !inside;
                buffer.append(ch);
                continue;
            }

            if (inside)
                buffer.append(ch);
            else
                if (ch == '{') {
                    indent += space;
                    buffer.append(ch + "\n" + indent);
                } else if (ch == '}' || ch == ']') {
                    indent = indent.substring(0, indent.length() - spacing);
                    buffer.append("\n" + indent + ch);
                } else if (ch == '[') {
                    indent += space;
                    buffer.append(ch);
                } else if (ch == ',')
                    buffer.append(ch + "\n" + indent);
                else if (ch == ':')
                    buffer.append(ch + " ");
                else
                    buffer.append(ch);
        }
        return buffer.toString();
    }
}
