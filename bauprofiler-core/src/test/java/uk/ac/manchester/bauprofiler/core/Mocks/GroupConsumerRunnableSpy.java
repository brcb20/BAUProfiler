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

import java.util.concurrent.atomic.AtomicInteger;

public class GroupConsumerRunnableSpy implements GroupConsumerRunnable {
    private final MultiGroupConsumer parentConsumer;
    private final Object controlLock = new Object();
    private volatile int terminateCount = 0;
    private volatile int resurrectCount = 0;
    private volatile boolean alive = true;
    private volatile boolean terminationRequested = false;
    private volatile boolean ready = false;
    private AtomicInteger releaseCount = new AtomicInteger();

    public GroupConsumerRunnableSpy(MultiGroupConsumer parentConsumer) {
	this.parentConsumer = parentConsumer;
    }

    public void run() {
	while (!terminationRequested) {
	    waitForReleaseFromCtrl();
	    parentConsumer.execute();
	}
	alive = false;
    }

    private void waitForReleaseFromCtrl() {
	if (releaseCount.getAndDecrement() == 0) {
	    synchronized (controlLock) {
		ready = true;
		try {
		    controlLock.wait();
		} catch (InterruptedException e) {
		    // ignore
		}
		ready = false;
	    }
	}
    }

    public void resurrect() {
	++resurrectCount;
	terminationRequested = false;
	alive = true;
    }

    public void requestTermination() {
	++terminateCount;
	terminationRequested = true;
    }

    public void releaseFromCtrl(int times) {
	if (times < 1)
	    throw new RuntimeException("Invalid release increment");

	if (releaseCount.getAndAdd(times) == -1) {
	    waitUntilReadyForCtrl();
	    synchronized (controlLock) {
		controlLock.notify();
	    }
	}
    }

    private void waitUntilReadyForCtrl() {
	while (!ready) {
	    // busy wait
	}
    }

    public boolean isAlive() {
	return alive;
    }

    public int getResurrectCount() {
	return resurrectCount;
    }

    public int getTerminateCount() {
	return terminateCount;
    }
}
