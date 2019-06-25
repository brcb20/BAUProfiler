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

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

import uk.ac.manchester.bauprofiler.core.interfaces.Timed;
import uk.ac.manchester.bauprofiler.core.converter.ConvertableProfile;

public class ProfilerImplementation extends Profiler {
    private static final Link EMPTY_LINK = null;

    private final Set<Long> attachedGroupIds = Collections.synchronizedSet(new HashSet<>());
    private final ConcurrentHashMap<Link, List<ConvertableProfile>> cachedProfiles =
        new ConcurrentHashMap<>();
    private final Set<Long> disabledGroupIds = Collections.synchronizedSet(new HashSet<>());
    private final ConcurrentHashMap<Long, Link> hardLinks = new ConcurrentHashMap<>();

    private final ProfileDispatcher dispatcher;

    protected ProfilerImplementation(ProfileDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void enable(long groupId) {
        disabledGroupIds.remove(groupId);
    }

    @Override
    public void disable(long groupId) {
        disabledGroupIds.add(groupId);
    }

    @Override
    public <T> void link(long groupId, T hardLink) {
        if (hardLink == null)
            throw new NullPointerException();

        hardLinks.put(groupId, substituteLink(hardLink));
    }

    private <T> Link substituteLink(T link) {
        return new Link(link.hashCode());
    }

    @Override
    public void unlink(long groupId) {
        hardLinks.remove(groupId);
    }

    @Override
    public void attach(long groupId) {
        if (isAttached(groupId))
            throw new AttachException(groupId);

        attachedGroupIds.add(groupId);
        dispatcher.dispatchGroup(groupId);
    }

    private boolean isAttached(long groupId) {
        return attachedGroupIds.contains(groupId);
    }

    @Override
    public <T> void attach(long groupId, T link) {
        if (link == null)
            throw new NullPointerException();

        attach(groupId);
        dispatchCachedProfilesToGroup(substituteLink(link), groupId);
    }

    private void dispatchCachedProfilesToGroup(Link link, long groupId) {
        if (cachedProfiles.containsKey(link))
            for (ConvertableProfile p : cachedProfiles.get(link))
                dispatchPreparedProfileToGroup(p, groupId);
    }

    @Override
    public void detach(long groupId) {
        if (!isAttached(groupId))
            throw new DetachException(groupId);

        dispatcher.releaseGroup(groupId);
        attachedGroupIds.remove(groupId);
    }

    @Override
    public void clean() {
        cachedProfiles.clear();
    }

    @Override
    public <T extends ConvertableProfile & Timed> Timer tprofile(T profile, long groupId) {
        profile(profile, groupId);
        return timeProfile(profile);
    }

    private TimedAction timeProfile(Timed profile) {
        TimedAction timer = new TimedAction();
        profile.setTimer(timer);
        return timer;
    }

    @Override
    public void profile(ConvertableProfile profile, long groupId) {
        dispatchOrCacheProfile(profile, groupId, EMPTY_LINK);
    }

    private <T> void dispatchOrCacheProfile(
            ConvertableProfile profile, long groupId, T softLink) {
        if (isDisabled(groupId))
            return;

        if (isAttached(groupId))
            dispatchPreparedProfileToGroup(profile, groupId);
        else if (isHardLinked(groupId))
            cacheProfile(profile, getHardLink(groupId));
        else if (isNotEmptyLink(softLink))
            cacheProfile(profile, substituteLink(softLink));
    }

    private boolean isDisabled(long groupId) {
        return disabledGroupIds.contains(groupId);
    }

    private void dispatchPreparedProfileToGroup(ConvertableProfile profile, long groupId) {
        prepareProfile(profile);
        dispatcher.dispatchProfileToGroup(profile, groupId);
    }

    private void prepareProfile(ConvertableProfile profile) {
        profile.setVerbosity(Profiler.VERBOSE);
        profile.preProcess();
    }

    private boolean isHardLinked(long groupId) {
        return hardLinks.containsKey(groupId);
    }

    private <T> void cacheProfile(ConvertableProfile profile, Link link) {
        if (!cachedProfiles.containsKey(link))
            cachedProfiles.put(link, new ArrayList<ConvertableProfile>());
        cachedProfiles.get(link).add(profile);
    }

    private Link getHardLink(long groupId) {
        return hardLinks.get(groupId);
    }

    private <T> boolean isNotEmptyLink(T link) {
        return link != EMPTY_LINK;
    }

    @Override
    public <T1 extends ConvertableProfile & Timed, T2> Timer tprofile(
            T1 profile, long groupId, T2 softLink) {
        profile(profile, groupId, softLink);
        return timeProfile(profile);
    }

    @Override
    public <T> void profile(ConvertableProfile profile, long groupId, T softLink) {
        if (softLink == null)
            throw new NullPointerException();

        dispatchOrCacheProfile(profile, groupId, softLink);
    }

    public static class AttachException extends RuntimeException {
        public AttachException(long groupId) {
            super(groupId+" attached multiple times");
        }
    }

    public static class DetachException extends RuntimeException {
        public DetachException(long groupId) {
            super(groupId+" not attached");
        }
    }
}
