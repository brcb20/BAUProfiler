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
package uk.ac.manchester.bauprofiler.core;

import java.util.Optional;

import uk.ac.manchester.bauprofiler.core.converter.ConvertableProfile;
import uk.ac.manchester.bauprofiler.core.converter.Conversion;
import uk.ac.manchester.bauprofiler.core.assembler.AssemblyNode;

public class DummyConvertableProfile implements ConvertableProfile {
    public int getId() {
	return 0;
    }

    public Optional<Integer> getDependencyId() {
	return Optional.empty();
    }

    public boolean predicate(Profile dep) {
	return false;
    }

    public void setVerbosity(boolean verbose) {
    }

    public Conversion convert() {
	return new Conversion() {
	    public long id() {
		return 0;
	    }

	    public String toString() {
		return "";
	    }

	    public AssemblyNode[] getAssemblyNodes() {
		return new AssemblyNode[]{};
	    }
	};
    }
}
