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

public class ProfilerImplementationTest {
    private DispatcherSpy dispatcherSpy;
    private ProfilerImplementation profiler;

    @Before
    public void setup() {
	dispatcherSpy = new DispatcherSpy();
	profiler = new ProfilerImplementation(dispatcherSpy);
    }

    @Test (expected=ProfilerImplementation.DetachException.class)
    public void testDetachBeforeAttach() {
	profiler.detach(10L);
    }

    @Test (expected=ProfilerImplementation.AttachException.class)
    public void testAttachTwice() {
	profiler.attach(10L);
	profiler.attach(10L);
    }

    @Test (expected=NullPointerException.class)
    public void testNullCannotBeUsedAsCacheLink() {
	profiler.attach(10L, null);
    }

    @Test
    public void testProfileIsNotCachedWithoutLink() {
	long groupId = 10L;
	profiler.profile(new DummyConvertableProfile(), groupId);
	profiler.attach(groupId);

	assertThat(dispatcherSpy.getCallSummary(), equalTo("dg"+groupId));
    }

    @Test (expected=NullPointerException.class)
    public void testProfileWithNullLink() {
	profiler.profile(new DummyConvertableProfile(), 10L, null);
    }

    @Test
    public void testProfileCachedUnderSoftLink() {
	long groupId = 10L;
	Object softLink = new Object();
	profiler.profile(new DummyConvertableProfile(), groupId, softLink);
	profiler.attach(groupId, softLink);

	assertThat(dispatcherSpy.getCallSummary(), equalTo(
		    "dg"+groupId+"dpg"+groupId+"[DummyConvertableProfile]"));
    }

    @Test (expected=NullPointerException.class)
    public void testNullLink() {
	profiler.link(10L, null);
    }

    @Test
    public void testProfileCachedUnderHardLinkWhenProfileWithoutLink() {
	long groupId = 10L;
	Object link = new Object();
	profiler.link(groupId, link);
	profiler.profile(new DummyConvertableProfile(), groupId);

	profiler.attach(groupId, link);
	assertThat(dispatcherSpy.getCallSummary(), equalTo(
		    "dg"+groupId+"dpg"+groupId+"[DummyConvertableProfile]"));
    }

    @Test
    public void testHardLinkingOverridesProfileWithLink() {
	long groupId = 10L;
	Object link = new Object(), otherLink = new Object();
	profiler.link(groupId, link);
	profiler.profile(new DummyConvertableProfile(), groupId, otherLink);

	profiler.attach(groupId, link);
	assertThat(dispatcherSpy.getCallSummary(), equalTo(
		    "dg"+groupId+"dpg"+groupId+"[DummyConvertableProfile]"));
    }

    @Test
    public void testProfileWithoutLinkIsDispatchedWhenGroupIsAlreadyAttached() {
	long groupId = 10L;
	profiler.attach(groupId);

	profiler.profile(new DummyConvertableProfile(), groupId);

	assertThat(dispatcherSpy.getCallSummary(), equalTo(
		    "dg"+groupId+"dpg"+groupId+"[DummyConvertableProfile]"));
    }

    @Test
    public void testProfileWithLinkIsDispatchedWhenGroupIsAlreadyAttached() {
	long groupId = 10L; Object link = new Object();
	profiler.attach(groupId);

	profiler.profile(new DummyConvertableProfile(), groupId, link);

	assertThat(dispatcherSpy.getCallSummary(), equalTo(
		    "dg"+groupId+"dpg"+groupId+"[DummyConvertableProfile]"));
    }

    @Test
    public void testCachedProfilesPersistAfterDetach() {
	long groupId = 10L;
	Object link = new Object();
	String summaryUpToDetach = "dg"+groupId+"dpg"+groupId+"[DummyConvertableProfile]"
	    , summaryOfDetach = "rg"+groupId
	    , summaryAfterDetach = summaryUpToDetach;

	profiler.profile(new DummyConvertableProfile(), groupId, link);
	profiler.attach(groupId, link);
	profiler.detach(groupId);

	profiler.attach(groupId, link);

	assertThat(dispatcherSpy.getCallSummary()
		, equalTo(summaryUpToDetach+summaryOfDetach+summaryAfterDetach));
    }

    @Test
    public void testCachingStopsAfterUnlinking() {
	long groupId = 10L;
	Object link = new Object();

	profiler.link(groupId, link);
	profiler.profile(new DummyConvertableProfile(), groupId);

	profiler.unlink(groupId);
	profiler.profile(new DummyConvertableProfile(), groupId);
	profiler.attach(groupId, link);

	assertThat(dispatcherSpy.getCallSummary()
		, equalTo("dg"+groupId+"dpg"+groupId+"[DummyConvertableProfile]"));
    }

    @Test
    public void testDisablingGroupPreventsProfiling() {
	long groupId = 10L;
	profiler.disable(groupId);

	profiler.attach(groupId);
	profiler.profile(new DummyConvertableProfile(), groupId);

	assertThat(dispatcherSpy.getCallSummary(), equalTo("dg"+groupId));
    }

    @Test
    public void testDisablingGroupPreventsCachingProfiles() {
	long groupId = 10L;
	Object link = new Object();

	profiler.disable(groupId);

	profiler.profile(new DummyConvertableProfile(), groupId, link);
	profiler.attach(groupId, link);

	assertThat(dispatcherSpy.getCallSummary(), equalTo("dg"+groupId));
    }
}
