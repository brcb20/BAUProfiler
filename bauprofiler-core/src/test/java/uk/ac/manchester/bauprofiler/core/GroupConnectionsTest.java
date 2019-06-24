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

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.Test;
import org.junit.Before;

import java.util.Arrays;

public class GroupConnectionsTest {
    private GroupConnections connections;

    @Before
    public void setup() {
	connections = new GroupConnections();
    }

    @Test
    public void testConnection() {
	connections.connect(10L);

	assertThat(connections.getNumOfConnections(), equalTo(1));
    }

    @Test
    public void testConnectionAndDisconnection() {
	connections.connect(10L);
	connections.serviceConnection();

	connections.disconnect();

	assertThat(connections.getNumOfConnections(), equalTo(0));
    }


    @Test (expected = GroupConnections.ServicedConnectionException.class)
    public void testDisconnecting_whenNoConnections() {
	connections.disconnect();
    }

    @Test (expected = GroupConnections.ServicedConnectionException.class)
    public void testDisconnectingTwice_afterOneServicedConnection() {
	connections.connect(10L);
	connections.serviceConnection();

	connections.disconnect();
	connections.disconnect();
    }

    @Test (expected = GroupConnections.UnavailableConnectionException.class)
    public void testServicingUnavailableConnection() {
	connections.serviceConnection();
    }

    @Test (expected = GroupConnections.UnavailableConnectionException.class)
    public void testServicingSecondConnection_whenOnlyOneAvailable() {
	connections.connect(10L);

	connections.serviceConnection();
	connections.serviceConnection();
    }

    @Test
    public void testServicingConnectionsOccursInSameOrderAsConnected() {
	long groupIds[] = new long[]{1L, 2L, 3L, 4L};
	connectMultipleGroupIds(groupIds);

	long[] servicedGroupIds = serviceAllConnectionsOnce();
	assertThat(servicedInSameOrderAsConnected(groupIds, servicedGroupIds), equalTo(true));

	servicedGroupIds = serviceAllConnectionsOnce();
	assertThat(servicedInSameOrderAsConnected(groupIds, servicedGroupIds), equalTo(true));
    }

    private void connectMultipleGroupIds(long[] groupIds) {
	for (int i = 0; i < groupIds.length; i++)
	    connections.connect(groupIds[i]);
    }

    private long[] serviceAllConnectionsOnce() {
	long[] servicedGroupIds = new long[connections.getNumOfConnections()];
	for (int i = 0; i < servicedGroupIds.length; i++)
	    servicedGroupIds[i] = connections.serviceConnection();
	return servicedGroupIds;
    }

    private boolean servicedInSameOrderAsConnected(long[] groupIds, long[] servicedGroupIds) {
	return Arrays.equals(groupIds, servicedGroupIds);
    }
}
