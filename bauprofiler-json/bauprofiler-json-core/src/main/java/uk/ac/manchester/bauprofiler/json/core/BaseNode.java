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
package uk.ac.manchester.bauprofiler.json.core;

import uk.ac.manchester.bauprofiler.core.assembler.AssemblyNode;

public class BaseNode implements AssemblyNode {
    private final String prefix;
    private final long uniqueId;
    private final String preOpen;
    private final String open;
    private final String close;
    private final String postClose;
    private final String separator = ":";

    public BaseNode(String prefix, long uniqueId
            , String preOpen, String open, String close, String postClose) {
        this.prefix = prefix;
        this.uniqueId = uniqueId;
        this.preOpen = preOpen;
        this.open = open;
        this.close = close;
        this.postClose = postClose;
    }

    public String prefix() {
        return prefix;
    }

    public long uniqueId() {
        return uniqueId;
    }

    public String preOpen() {
        return preOpen;
    }

    public String open() {
        return open;
    }

    public String close()  {
        return close;
    }

    public String postClose() {
        return postClose;
    }

    public String separator() {
        return separator;
    }
}
