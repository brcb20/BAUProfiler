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

public class MultiGroupConsumerState {
    public enum State {
        NEW,
        RUNNING,
        WAIT,
        TERMINATED
    }

    private State state = State.NEW;

    public synchronized State get() {
        return state;
    }

    public synchronized void set(State newValue) {
        state = newValue;
    }

    public synchronized State getAndSet(State newValue) {
        State tmp = state;
        state = newValue;
        return tmp;
    }

    public synchronized boolean compareAndSet(State expected, State update) {
        if (state == expected) {
            state = update;
            return true;
        }
        return false;
    }
}
