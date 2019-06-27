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
        private final HashMap<Class<ConvertableProfile>, Deque<ConvertableProfile>>
            profilesWithDeps = new HashMap<>();
        private final List<ConvertableProfile> orderedProfiles = new LinkedList<>();

        private Builder() {}

        public void insert(ConvertableProfile profile) {
            Class<ConvertableProfile> dependency = profile.depends();
            if (dependency == null)
                profiles.add(profile);
            else
                insertProfileWithDep(profile, dependency);
        }

        private void insertProfileWithDep(ConvertableProfile profile, Class dependency) {
            Deque<ConvertableProfile> profilesWithSameDep = profilesWithDeps.get(dependency);
            if (profilesWithSameDep == null) {
                profilesWithSameDep = new LinkedList<ConvertableProfile>();
                profilesWithDeps.put(dependency, profilesWithSameDep);
            }
            profilesWithSameDep.add(profile);
        }

        public OrderedProfileQueue build() {
            orderProfiles();
            return new OrderedProfileQueue(this);
        }

        private void orderProfiles() {
            while (profiles.size() > 0) {
                ConvertableProfile profile = profiles.removeFirst();
                Deque<ConvertableProfile> dependents = profilesWithDeps.get(profile.getClass());
                if (dependents != null
                        && dependents.size() != 0
                        && dependents.peek().predicate(profile))
                    profiles.addFirst(dependents.removeFirst());
                orderedProfiles.add(profile);
            }
        }
    }
}
