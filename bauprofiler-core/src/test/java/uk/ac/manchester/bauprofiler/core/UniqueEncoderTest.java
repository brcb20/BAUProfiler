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

public class UniqueEncoderTest {
    private UniqueEncoder encoder;

    private boolean doesNotHaveDuplicates(long[] elements) {
	for (int i = 0; i < elements.length; i++)
	    for (int j = i+1; j < elements.length; j++)
		if (elements[j] == elements[i])
		    return false;
	return true;
    }

    private void encodeSameGroupIdTwice(long groupId) {
	encoder.encode(groupId);
	encoder.encode(groupId);
    }

    private long[] reencodeSameGroupId(long groupId, int times) {
	long[] uniqueIds = new long[times];
	for (int i = 0; i < times; i++) {
	    encoder.encode(groupId);
	    uniqueIds[i] = encoder.getEncoding(groupId);
	    encoder.remove(groupId);
	}
	return uniqueIds;
    }

    private void encodeIncrementalGroupIds(long startFrom, long times) {
	for (long i = startFrom; i < startFrom+times; i++)
	    encoder.encode(i);
    }

    @Before
    public void setup() {
	encoder = UniqueEncoder.boundedEncoder(0, 1);
    }

    @Test (expected=UniqueEncoder.BoundaryException.class)
    public void testMinCannotBeGreaterThanMax_whenCreatingBounded() {
	encoder = UniqueEncoder.boundedEncoder(10, 3);
    }

    @Test (expected=UniqueEncoder.BoundaryException.class)
    public void testMinCannotBeLessThanZero_whenCreatingBounded() {
	encoder = UniqueEncoder.boundedEncoder(-1, 3);
    }

    @Test (expected=UniqueEncoder.ReencodingException.class)
    public void testPreventsReencodingBeforeRemoving() {
	encodeSameGroupIdTwice(10L);
    }

    @Test (expected=UniqueEncoder.NoSuchEncodingException.class)
    public void testGettingEncoding_beforeEncodingIt() {
	encoder.getEncoding(1L);
    }

    @Test (expected=UniqueEncoder.NoSuchEncodingException.class)
    public void testRemovingEncoding_beforeEncodingIt() {
	encoder.remove(1L);
    }

    @Test (expected=UniqueEncoder.FullCapacityException.class)
    public void testCapacityOverflow() {
	encodeIncrementalGroupIds(1L, 10L);
    }

    @Test
    public void testEncodesSameGroupIdToDifferentUniqueId_whenNoLoopAround() {
	encoder = UniqueEncoder.unboundedEncoder();
	long[] uniqueIds = reencodeSameGroupId(10L, 15);
	assertThat(doesNotHaveDuplicates(uniqueIds), equalTo(true));
    }

    @Test
    public void testLoopsAround_whenReachesMaxUniqueId() {
	long[] uniqueIds = reencodeSameGroupId(1L, 4);
	assertThat(uniqueIds[0], equalTo(uniqueIds[2]));
	assertThat(uniqueIds[1], equalTo(uniqueIds[3]));
    }

    public void testFindsNextAvailableUniqueId_afterRemovalOfOneFromFullCapacity() {
	encoder = UniqueEncoder.boundedEncoder(0, 4);
	encodeIncrementalGroupIds(1L, 5L);
	Long uniqueId = encoder.getEncoding(4L);
	encoder.remove(4L);

	encoder.encode(6L);

	assertThat(encoder.getEncoding(6L), equalTo(uniqueId));
    }
}
