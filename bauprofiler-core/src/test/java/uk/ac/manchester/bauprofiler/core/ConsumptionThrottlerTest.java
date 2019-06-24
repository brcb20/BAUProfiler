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

public class ConsumptionThrottlerTest {
    private GroupConsumer throttler;
    private BackoffSpy backoff;
    private ManuallyPopulatedGroupConsumerStub grouping;

    private void givenPopulatedFinalGroups(long[] groupIds, int quantity) {
	populateGroups(groupIds, quantity);
	markGroupsAsFinal(groupIds);
    }

    private void populateGroups(long[] groupIds, int quantity) {
	for (int i = 0; i < groupIds.length; i++)
	    grouping.populateGroup(groupIds[i], quantity);
    }

    private void markGroupsAsFinal(long[] finalGroupIds) {
	for (int i = 0; i < finalGroupIds.length; i++)
	    grouping.markGroupAsFinal(finalGroupIds[i]);
    }

    private void whenConsumedViaThrottler() {
	whenPartiallyConsumedViaThrottler(grouping.getNumOfConsumedGroups(), 0);
    }

    private void whenPartiallyConsumedViaThrottler(int numOfGroups, int start) {
	Long[] groupIds = grouping.getConsumedGroupIds().toArray(
		new Long[grouping.getNumOfConsumedGroups()]);
	for (int i = start; i < start+numOfGroups; i++) {
	    consumeAllInGroup(groupIds[i]);
	    if (throttler.isMarkedAsFinalGroup(groupIds[i]))
		throttler.deleteGroup(groupIds[i]);
	}
    }

    private void consumeAllInGroup(long groupId) {
	while (throttler.hasNextInGroup(groupId)) {
	    throttler.getNextFromGroup(groupId);
	}
    }

    private void thenBackoffShouldBeUnnecessary() {
	assertThat(sumOfBackoffCounts(), equalTo(0));
    }

    private int sumOfBackoffCounts() {
	return backoff.getBackoffCount()
	    + backoff.getIncreaseCount()
	    + backoff.getResetCount();
    }

    private void whenConsumedViaThrottlerMultipleTimes(int times) {
	for (int i = 0; i < times; i++)
	    whenConsumedViaThrottler();
    }

    private void thenBackoffShouldOccur(int expect) {
	assertThat(backoff.getBackoffCount(), equalTo(expect));
    }

    @Before
    public void setup() {
	backoff = new BackoffSpy();
	grouping = new ManuallyPopulatedGroupConsumerStub();
	throttler = new ConsumptionThrottler(grouping, backoff);
    }

    @Test
    public void testNoUnnecessaryBackoffOccurs_whenAllConsumedInFirstIteration() {
	givenPopulatedFinalGroups(new long[]{1L, 2L, 3L, 4L}, 5);

	whenConsumedViaThrottler();

	thenBackoffShouldBeUnnecessary();
    }

    @Test
    public void testNoBackoff_on1stIterForEmptyGroup() {
	populateGroups(new long[]{1L}, 0);

	whenConsumedViaThrottlerMultipleTimes(1);

	thenBackoffShouldOccur(0);
    }

    @Test
    public void testBackoffOccursOn2ndFailedIterationForEmptyGroup() {
	populateGroups(new long[]{1L}, 0);

	whenConsumedViaThrottlerMultipleTimes(2);

	thenBackoffShouldOccur(1);
    }

    @Test
    public void testBackoffOccurs_afterConsumptionOfGroupAndAttemptToConsumeAgain() {
	int failedConsumptionIterations = 5;
	populateGroups(new long[]{1L}, 5);
	whenConsumedViaThrottler();

	whenConsumedViaThrottlerMultipleTimes(failedConsumptionIterations);

	thenBackoffShouldOccur(failedConsumptionIterations);
    }

    // refactor and make more expressive
    // maybe create a hierarchical test with pre-consumption as the hierarchy
    // if this is the first time a groupid is throttled and hasNext == false
    // backoff should not occur!
    // successiveFailedTries should not even be incremented!
    @Test
    public void testBackoffOccursOnlyOnce_whenAllGroupsMarkedFinalAfterFailedIteration() {
	int failedConsumptionIterations = 1;
	long[] groupIds = new long[]{1L, 2L, 3L, 4L};
	populateGroups(groupIds, 0);
	whenConsumedViaThrottlerMultipleTimes(failedConsumptionIterations);

	markGroupsAsFinal(groupIds);
	whenConsumedViaThrottler();

	assertThat(backoff.getBackoffCount(), equalTo(failedConsumptionIterations));
    }

    @Test
    public void testBackoffResetOccurs_whenGroupsRepopulatedMidFailedIteration() {
	long[] groupIds = new long[]{1L, 2L, 3L, 4L};
	populateGroups(groupIds, 5);
	whenConsumedViaThrottler();
	whenPartiallyConsumedViaThrottler(groupIds.length/2, 0);

	populateGroups(groupIds, 5);
	whenPartiallyConsumedViaThrottler(1, 2);

	assertThat(backoff.getResetCount(), equalTo(1));	
    }
}
