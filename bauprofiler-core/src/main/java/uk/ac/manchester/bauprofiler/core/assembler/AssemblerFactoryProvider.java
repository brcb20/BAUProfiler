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
package uk.ac.manchester.bauprofiler.core.assembler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class AssemblerFactoryProvider {
    public static AssemblerFactory loadFactory(String fullClassName, String[] params) {
        AssemblerFactory factory = null;
        try {
            Class<?> klass = Class.forName(fullClassName);
            Constructor<?> constructor = klass.getConstructor(String[].class);
            factory = (AssemblerFactory) constructor.newInstance(new Object[] {params});
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "[ERROR] Assembler Factory Implementation class not found");
        } catch (InstantiationException e) {
            throw new RuntimeException(
                    "[ERROR] Unable to instantiate Assembler Factory Implementation");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                    "[ERROR] Cannot find Constructor with parameter type String[].class for "
                    + "Assembler Factory Implementation");
        } catch (IllegalAccessException e) {
            throw new RuntimeException(
                    "[ERROR] Unable to access constructor of Assembler Factory Implementation");
        } catch (InvocationTargetException e) {
            throw new RuntimeException(
                    "[ERROR] Constructor of Assembler Factory Implementation "
                    + "threw an exception");
        }
        return factory;
    }
}
