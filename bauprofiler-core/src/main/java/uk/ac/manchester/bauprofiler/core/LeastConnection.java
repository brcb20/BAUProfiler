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

public class LeastConnection implements SchedulingPolicy {
    public int selectConsumer(MultiGroupConsumer[] multiGroupConsumers) {
        return selectConsumerWithLeastConnections(multiGroupConsumers);
    }

    private int selectConsumerWithLeastConnections(MultiGroupConsumer[] mgcs) {
        for (int i = 0; i < mgcs.length; i++)
            if (mgcs[i].isAlive())
                return selectConsumerWithLeastConnectionsStartingFrom(mgcs, i);
        throw new SchedulingException();
    }

    private int selectConsumerWithLeastConnectionsStartingFrom(
            MultiGroupConsumer[] mgcs, int start) {
        int least = start;
        for (int i = start+1; i < mgcs.length; i++) {
            if (!mgcs[i].isAlive())
                continue;
            if (mgcs[i].getNumOfConnections() < mgcs[least].getNumOfConnections())
                least = i;
        }
        return least;
    }
}
