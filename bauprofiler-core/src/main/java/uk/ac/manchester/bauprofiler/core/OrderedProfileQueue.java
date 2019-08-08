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

import java.util.HashMap;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;
import java.util.Optional;

import uk.ac.manchester.bauprofiler.core.converter.ConvertableProfile;

public class OrderedProfileQueue {
    private final List<ConvertableProfile> orderedProfiles;

    private OrderedProfileQueue(Builder builder) {
        this.orderedProfiles = Collections.unmodifiableList(builder.orderedProfiles);
    }

    public List<ConvertableProfile> getProfiles() {
        return orderedProfiles;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Deque<ConvertableProfile> profiles = new LinkedList<>();
        private final HashMap<Integer, Deque<ConvertableProfile>> profilesWithDeps =
            new HashMap<>();
        private final HashMap<Integer, Deque<ConvertableProfile>> profilesWithSelfDeps =
            new HashMap<>();
        private final List<ConvertableProfile> orderedProfiles = new LinkedList<>();

        private Builder() {}

        public void insert(ConvertableProfile profile) {
            Optional<Integer> dependencyId = profile.getDependencyId();
            if (dependencyId.isPresent())
                insertDependentProfile(profile, dependencyId.get());
            else
                profiles.add(profile);
        }

        private void insertDependentProfile(ConvertableProfile profile, Integer dependencyId) {
            if (isSelfReferentialDep(profile, dependencyId))
                insertProfileWithSelfDep(profile, dependencyId);
            else
                insertProfileAsDependent(profile, profilesWithDeps, dependencyId);
        }

        private boolean isSelfReferentialDep(ConvertableProfile profile, Integer dependencyId) {
            return profile.getId() == dependencyId;
        }

        private void insertProfileWithSelfDep(ConvertableProfile profile, Integer dependencyId) {
            if (!profilesWithSelfDeps.containsKey(dependencyId))
                profiles.add(new ConvertableProfilePlaceholder(dependencyId));
            insertProfileAsDependent(profile, profilesWithSelfDeps, dependencyId);
        }

        private void insertProfileAsDependent(
                ConvertableProfile profile
                , HashMap<Integer, Deque<ConvertableProfile>> dependencyMap
                , Integer dependencyId) {
            Deque<ConvertableProfile> profilesWithSameDep = dependencyMap.get(dependencyId);
            if (profilesWithSameDep == null) {
                profilesWithSameDep = new LinkedList<ConvertableProfile>();
                dependencyMap.put(dependencyId, profilesWithSameDep);
            }
            profilesWithSameDep.add(profile);
        }

        public OrderedProfileQueue build() {
            orderProfiles();
            return new OrderedProfileQueue(this);
        }

        private void orderProfiles() {
            while (profiles.size() > 0) {
                if (profiles.getFirst() instanceof ConvertableProfilePlaceholder) {
                    orderAllProfilesAndDeps(profilesWithSelfDeps.get(
                                profiles.removeFirst().getId()));
                    continue;
                }
                orderSingleProfileAndDep(profiles);
            }
        }

        private void orderAllProfilesAndDeps(Deque<ConvertableProfile> currentProfiles) {
            while (currentProfiles.size() > 0) {
                orderSingleProfileAndDep(currentProfiles);
            }
        }

        private void orderSingleProfileAndDep(Deque<ConvertableProfile> currentProfiles) {
            ConvertableProfile profile = currentProfiles.removeFirst();
            Deque<ConvertableProfile> dependents = profilesWithDeps.get(profile.getId());
            if (dependents != null
                    && dependents.size() != 0
                    && dependents.peek().dependsOn(profile))
                currentProfiles.addFirst(dependents.removeFirst());
            orderedProfiles.add(profile);
        }
    }
}
