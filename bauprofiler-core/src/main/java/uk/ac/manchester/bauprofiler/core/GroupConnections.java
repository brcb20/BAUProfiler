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

import java.util.Queue;
import java.util.ArrayDeque;

public class GroupConnections {
    private final Queue<Long> connectedGroupIds = new ArrayDeque<>();
    private int connectionCount = 0;
    private Long servicedGroupId = null;

    public synchronized void connect(Long groupId) {
        connectedGroupIds.add(groupId);
        ++connectionCount;
    }

    public synchronized Long serviceConnection() {
        if(hasNoAvailableConnections())
            throw new UnavailableConnectionException();
        updateServicedGroupId();
        return servicedGroupId;
    }

    private boolean hasNoAvailableConnections() {
        return getNumOfConnections() == 0
            || (servicedGroupId != null && getNumOfConnections() == 1);
    }

    private void updateServicedGroupId() {
        if (servicedGroupId != null) {
            connectedGroupIds.add(servicedGroupId);
        }
        servicedGroupId = connectedGroupIds.poll();
    }

    public synchronized void disconnect() {
        if (isNotServicingConnection())
            throw new ServicedConnectionException();
        servicedGroupId = null;
        --connectionCount;
    }

    private boolean isNotServicingConnection() {
        return servicedGroupId == null;
    }

    public synchronized int getNumOfConnections() {
        return connectionCount;
    }

    public static class UnavailableConnectionException extends RuntimeException {
    }

    public static class ServicedConnectionException extends RuntimeException {
    }
}
