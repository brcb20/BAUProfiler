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
package uk.ac.manchester.bauprofiler.json.generator;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class PersistentEncoder implements Encoder {
    private static final String filename = "./etc/persistent_encoding.so";
    private EncoderImpl impl;

    private PersistentEncoder(EncoderImpl impl) {
	this.impl = impl;
    }

    public static PersistentEncoder load() throws ClassNotFoundException, IOException {
	EncoderImpl impl = null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            impl = (EncoderImpl) ois.readObject();
        } catch (FileNotFoundException e) {
            // Not yet created
        }

        if (impl == null)
	    impl = new EncoderImpl();

	return new PersistentEncoder(impl);
    }

    public void persist() throws IOException {
	try (ObjectOutputStream oos =
		new ObjectOutputStream(new FileOutputStream(filename, false))) {
	    oos.writeObject(impl);
	}
    }

    public int encodePrefix(String in) {
	return impl.encodePrefix(in);
    }

    public int nextConversionID() {
	return impl.nextConversionID();
    }
}

