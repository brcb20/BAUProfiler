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

import uk.ac.manchester.bauprofiler.core.converter.ConvertableProfile;

public class ConcurrentProfileDispatcher implements ProfileDispatcher {
    private final GroupProducer grouping;
    private final Distributor distrib;
    private final UniqueEncoder encoder;

    protected ConcurrentProfileDispatcher(
            GroupProducer grouping, Distributor distrib, UniqueEncoder encoder) {
        this.grouping = grouping;
        this.distrib = distrib;
        this.encoder = encoder;
        distrib.prepareDistribChannel();
    }

    public void dispatchProfileToGroup(ConvertableProfile profile, Long groupId) {
        grouping.insertProfileIntoGroup(profile, encoder.getEncoding(groupId));
    }

    public void dispatchGroup(Long groupId) {
        encoder.encode(groupId);
        Long uniqueId = encoder.getEncoding(groupId);
        grouping.createGroup(uniqueId);
        distrib.distributeGroup(uniqueId);
    }

    public void releaseGroup(Long groupId) {
        Long uniqueId = encoder.getEncoding(groupId);
        grouping.markGroupAsFinal(uniqueId);
        encoder.remove(groupId);
    }
}
