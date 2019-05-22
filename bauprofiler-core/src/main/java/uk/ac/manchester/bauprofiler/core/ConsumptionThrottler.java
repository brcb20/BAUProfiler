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

import java.util.Set;
import java.util.HashSet;

import uk.ac.manchester.bauprofiler.core.converter.ConvertableProfile;

public class ConsumptionThrottler implements GroupConsumer {
    private final GroupConsumer proxiedGrouping;
    private final Backoff backoff;
    private final Set<Long> activeGIDs = new HashSet<>();

    private boolean newId;
    private boolean hasNext;
    private Long mostRecentlyUsedGID = null;
    private int mostRecentlyUsedCount = 0;
    private int successiveFailedTries = 0;
    private int successiveBackoffs = 0;

    public ConsumptionThrottler(GroupConsumer proxiedGrouping, Backoff backoff) {
        this.proxiedGrouping = proxiedGrouping;
        this.backoff = backoff;
    }

    public boolean hasNextInGroup(Long groupId) {
        hasNext = proxiedGrouping.hasNextInGroup(groupId);
        if (isNotMostRecentlyUsed(groupId)) {
            updateActiveGIDs(groupId);
            updateMostRecentlyUsed(groupId);
        }

        if (hasNext)
            updateProxyNextProfileInGroup(groupId);
        else
            updateProxyNoProfileInGroup(groupId);

        if (activeGIDsHaveNoNext())
            performBackoff();

        return hasNext;
    }

    private void updateProxyNextProfileInGroup(Long groupId) {
        if (proxyRequiresReset())
            resetProxy();
        ++mostRecentlyUsedCount;
    }

    private boolean isNotMostRecentlyUsed(Long groupId) {
        return mostRecentlyUsedGID != groupId;
    }

    private void updateActiveGIDs(Long groupId) {
        newId = activeGIDs.add(groupId);
    }

    private void updateMostRecentlyUsed(Long groupId) {
        mostRecentlyUsedGID = groupId;
        mostRecentlyUsedCount = 0;
    }

    private boolean proxyRequiresReset() {
        return mostRecentlyUsedCount == 0
            && (successiveBackoffs > 0 || successiveFailedTries > 0);
    }

    private void resetProxy() {
        successiveBackoffs = successiveFailedTries = 0;
        backoff.reset();
    }

    private void updateProxyNoProfileInGroup(Long groupId) {
        if (hasNoProfileOnFirstTry() && !newId)
            ++successiveFailedTries;
        mostRecentlyUsedGID = null;
    }

    private boolean hasNoProfileOnFirstTry() {
        return !hasNext && mostRecentlyUsedCount == 0;
    }

    private boolean activeGIDsHaveNoNext() {
        return successiveFailedTries > 0 && successiveFailedTries == activeGIDs.size();
    }

    private void performBackoff() {
        backoff.backoff();
        backoff.increase();
        ++successiveBackoffs;
        successiveFailedTries = 0;
    }

    public ConvertableProfile getNextFromGroup(Long groupId) {
        return proxiedGrouping.getNextFromGroup(groupId);
    }

    public void deleteGroup(Long groupId) {
        if (proxyRequiresReset())
            resetProxy();
        proxiedGrouping.deleteGroup(groupId);
        activeGIDs.remove(groupId);
    }

    public boolean isMarkedAsFinalGroup(Long groupId) {
        return proxiedGrouping.isMarkedAsFinalGroup(groupId);
    }
}
