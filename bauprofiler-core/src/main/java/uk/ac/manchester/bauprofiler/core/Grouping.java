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
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.NoSuchElementException;

import uk.ac.manchester.bauprofiler.core.converter.ConvertableProfile;

public class Grouping implements GroupProducer, GroupConsumer {
    private final ConcurrentHashMap<Long, Queue<ConvertableProfile>> groupings =
        new ConcurrentHashMap<>();
    private final ConcurrentHashMap.KeySetView<Long, Boolean> groupIdsMarkedAsFinal =
        ConcurrentHashMap.newKeySet();

    public void createGroup(Long groupId) {
        checkGroupAvailability(groupId);
        createNewGrouping(groupId);
    }

    private void checkGroupAvailability(Long groupId) {
        if (isCreatedGroup(groupId))
            throw new GroupRecreationException(groupId);
    }

    private boolean isCreatedGroup(Long groupId) {
        return groupings.containsKey(groupId);
    }

    private void createNewGrouping(Long groupId) {
        groupings.put(groupId, new LinkedList<ConvertableProfile>());
    }

    public void insertProfileIntoGroup(ConvertableProfile profile, Long groupId) {
        checkGroupIsCreated(groupId);
        safelyInsertProfileIntoGroup(profile, groupId);
    }

    private void checkGroupIsCreated(Long groupId) {
        if (!isCreatedGroup(groupId))
            throw new NoSuchGroupException(groupId);
    }

    public boolean hasNextInGroup(Long groupId) {
        return (isCreatedGroup(groupId) && isNotEmptyGroup(groupId));
    }

    public ConvertableProfile getNextFromGroup(Long groupId) {
        checkGroupIsCreated(groupId);
        return safelyRemoveProfileFromGroup(groupId);
    }

    public void deleteGroup(Long groupId) {
        checkIfGroupIsDeletable(groupId);
        groupings.remove(groupId);
        groupIdsMarkedAsFinal.remove(groupId);
    }

    public void markGroupAsFinal(Long groupId) {
        checkGroupIsCreated(groupId);
        groupIdsMarkedAsFinal.add(groupId);
    }

    public boolean isMarkedAsFinalGroup(Long groupId) {
        return groupIdsMarkedAsFinal.contains(groupId);
    }

    private void safelyInsertProfileIntoGroup(ConvertableProfile profile, Long groupId) {
        Queue<ConvertableProfile> group = groupings.get(groupId);
        synchronized(group) {
            group.add(profile);
        }
    }

    private ConvertableProfile safelyRemoveProfileFromGroup(Long groupId) {
        Queue<ConvertableProfile> group = groupings.get(groupId);
        synchronized(group) {
            ConvertableProfile cp;
            try {
                cp = group.remove();
            } catch (NoSuchElementException e) {
                throw new NoSuchProfileException(groupId);
            }
            return cp;
        }
    }

    private boolean isNotEmptyGroup(Long groupId) {
        return !isEmptyGroup(groupId);
    }

    private boolean isEmptyGroup(Long groupId) {
        return safelyGetGroupSize(groupId) == 0;
    }

    private int safelyGetGroupSize(Long groupId) {
        Queue<ConvertableProfile> group = groupings.get(groupId);
        synchronized(group) {
            return group.size();
        }
    }

    private void checkIfGroupIsDeletable(Long groupId) {
        if (!isCreatedGroup(groupId))
            throw new NoSuchGroupException(groupId);
        if (isNotEmptyGroup(groupId))
            throw new GroupNotEmptyException(groupId);
        if (!isMarkedAsFinalGroup(groupId))
            throw new GroupNotFinalException(groupId);
    }

    public static class GroupingException extends RuntimeException {
        public GroupingException(long groupId) {
            super(String.format("Caused by GroupId %d", groupId));
        }
    }

    public static class NoSuchGroupException extends GroupingException {
        public NoSuchGroupException(long groupId) {
            super(groupId);
        }
    }

    public static class GroupRecreationException extends GroupingException {
        public GroupRecreationException(long groupId) {
            super(groupId);
        }
    }

    public static class GroupNotEmptyException extends GroupingException {
        public GroupNotEmptyException(long groupId) {
            super(groupId);
        }
    }

    public static class GroupNotFinalException extends GroupingException {
        public GroupNotFinalException(long groupId) {
            super(groupId);
        }
    }

    public static class NoSuchProfileException extends GroupingException {
        public NoSuchProfileException(long groupId) {
            super(groupId);
        }
    }
}
