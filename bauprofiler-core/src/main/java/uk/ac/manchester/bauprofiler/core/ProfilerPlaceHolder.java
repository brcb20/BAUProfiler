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

import uk.ac.manchester.bauprofiler.core.interfaces.Timed;
import uk.ac.manchester.bauprofiler.core.converter.ConvertableProfile;

public class ProfilerPlaceHolder extends Profiler {

    private static final Timer ETA = new EmptyTimedAction();

    @Override public <T> void link(long groupId, T hardLink) {}
    @Override public void unlink(long groupId) {}
    @Override public void enable(long groupId) {}
    @Override public void disable(long groupId) {}
    @Override public void attach(long groupId) {}
    @Override public <T> void attach(long groupId, T link) {}
    @Override public void detach(long groupId) {}
    @Override public void clean() {}

    @Override
    public <T extends ConvertableProfile & Timed> Timer tprofile(T profile, long groupId) {
        return ETA;
    }

    @Override public void profile(ConvertableProfile profile, long groupId) {}

    @Override
    public <T1 extends ConvertableProfile & Timed, T2> Timer tprofile(
            T1 profile, long groupId, T2 softLink) {
        return ETA;
    }

    @Override public <T> void profile(ConvertableProfile profile, long groupId, T softLink) {}
}
