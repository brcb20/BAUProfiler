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

public class TerminatorSpy implements Terminator, Distributor {
    private volatile int totalActivelyConsumedGroups = 0;
    private volatile int tentativeTerminateCount = 0;

    public void notifyGroupConsumed() {
	--totalActivelyConsumedGroups;
    }

    public boolean isIdle() {
	return totalActivelyConsumedGroups == 0;
    }

    public void tentativeTerminate() {
	++tentativeTerminateCount;
    }

    public void prepareDistribChannel() {
    }

    public void distributeGroup(Long groupId) {
	++totalActivelyConsumedGroups;
    }

    public int getTentativeTerminateCount() {
	return tentativeTerminateCount;
    }
}
