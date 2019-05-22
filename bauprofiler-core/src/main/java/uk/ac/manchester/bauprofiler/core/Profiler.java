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

import java.util.Properties;

import uk.ac.manchester.bauprofiler.core.interfaces.Timed;
import uk.ac.manchester.bauprofiler.core.converter.ConvertableProfile;
import uk.ac.manchester.bauprofiler.core.assembler.AssemblerFactoryProvider;

public abstract class Profiler {

    private static final Properties settings = System.getProperties();

    /* Default configuration (mission critical) */
    public static final boolean ENABLED = Boolean.parseBoolean(
            settings.getProperty("profiler.enable", "false"));
    public static final boolean SERVER_OUTPUT = Boolean.parseBoolean(
            settings.getProperty("profiler.output.server", "false"));
    public static final int SERVER_PORT = Integer.parseInt(
            settings.getProperty("profiler.output.server.port", "4444"));
    public static final boolean FILE_OUTPUT = Boolean.parseBoolean(
            settings.getProperty("profiler.output.file", "false"));
    public static final String FILE_PATH = settings.getProperty(
            "profiler.output.file.path", "./var/profiler.json");
    public static final int MAX_PTHREADS = Integer.parseInt(
            settings.getProperty("profiler.max_pthreads", "2"));
    public static final String ASSEMBLER_FACTORY = settings.getProperty(
            "profiler.load.assembler.factory");

    /* Default configuration (help me help you) */
    public static final String ASSEMBLER_FACTORY_PARAMS = settings.getProperty(
            "profiler.params.assembler.factory");
    public static final boolean PRETTY_PRINT = Boolean.parseBoolean(
            settings.getProperty("profiler.output.prettyprint", "false"));
    public static final boolean VERBOSE = Boolean.parseBoolean(
            settings.getProperty("profiler.output.verbose", "false"));
    public static final boolean DEBUG = Boolean.parseBoolean(
            settings.getProperty("profiler.debug", "false"));

    private static final Profiler instance;

    static {
        if (ENABLED) {
            Grouping grouping = new Grouping();
            instance = new ProfilerImplementation(
                new ConcurrentProfileDispatcher(
                    grouping
                    , new MultiGroupConsumerPool(
                        new MultiGroupConsumerPrototypeImpl(
                            grouping
                            , AssemblerFactoryProvider.loadFactory(
                                ASSEMBLER_FACTORY
                                , ASSEMBLER_FACTORY_PARAMS.split("\\s*\\,\\s*"))
                            , ((PRETTY_PRINT) ?
                                PrinterProvider.prettyPrinter() : PrinterProvider.printer()))
                        , new LeastConnection()
                        , MAX_PTHREADS)
                    , UniqueEncoder.unboundedEncoder()
                    )
                );
        } else
            instance = new ProfilerPlaceHolder();
    }

    public static Profiler getInstance() {
        return instance;
    }

    public abstract <T> void link(long groupId, T hardLink);
    public abstract void unlink(long groupId);
    public abstract void enable(long groupId);
    public abstract void disable(long groupId);
    public abstract void attach(long groupId);
    public abstract <T> void attach(long groupId, T link);
    public abstract void detach(long groupId);
    public abstract void clean();
    public abstract <T extends ConvertableProfile & Timed> Timer tprofile(
            T profile, long groupId);
    public abstract void profile(ConvertableProfile profile, long groupId);
    public abstract <T1 extends ConvertableProfile & Timed, T2> Timer tprofile(
            T1 profile, long groupId, T2 softLink);
    public abstract <T> void profile(ConvertableProfile profile, long groupId, T softLink);
}
