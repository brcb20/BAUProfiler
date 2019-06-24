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

import java.util.Arrays;

import uk.ac.manchester.bauprofiler.core.converter.ConvertableProfile;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.junit.Before;

@RunWith(HierarchicalContextRunner.class)
public class GroupingTest {
    private Grouping grouping;

    @Before
    public void createGrouping() {
	grouping = new Grouping();
    }

    public class ProducerContext {
	private GroupProducer producer;

	private void createSameGroupTwice(long groupId) {
	    producer.createGroup(groupId);
	    producer.createGroup(groupId);
	}

	@Before
	public void createProducer() {
	    producer = grouping;
	}

	@Test (expected=Grouping.GroupRecreationException.class)
	public void testCreatingSameGroup() {
	    createSameGroupTwice(10L);
	}

	@Test (expected=Grouping.NoSuchGroupException.class)
	public void testInsertingProfileInNotCreatedGroup() {
	    producer.insertProfileIntoGroup(new DummyConvertableProfile(), 10L);
	}

	@Test(expected=Grouping.NoSuchGroupException.class)
	public void testMarkingAsFinalNotCreatedGroup() {
	    producer.markGroupAsFinal(10L);
	}
    }

    public class ConsumerContext {
	private GroupConsumer consumer;

	@Before
	public void createConsumer() {
	    consumer = grouping;
	}

	@Test
	public void testNotCreatedGroupHasNoNext() {
	    assertThat(consumer.hasNextInGroup(10L), equalTo(false));
	}

	@Test (expected=Grouping.NoSuchGroupException.class)
	public void testConsumptionOfEmptyNotCreatedGroup() {
	    consumer.getNextFromGroup(10L);
	}

	@Test (expected=Grouping.NoSuchGroupException.class)
	public void testDeletingNotCreatedGroup() {
	    consumer.deleteGroup(10L);
	}

	public class ConsumerWithProducerContext {
	    private GroupProducer producer;

	    @Before
	    public void createProducer() {
		producer = grouping;
	    }

	    @Test
	    public void testEmptyGroupHasNoNext() {
		producer.createGroup(10L);
		assertThat(consumer.hasNextInGroup(10L), equalTo(false));
	    }

	    @Test (expected=Grouping.NoSuchProfileException.class)
	    public void testConsumptionOfEmptyCreatedGroup() {
		producer.createGroup(10L);
		consumer.getNextFromGroup(10L);	
	    }

	    @Test (expected=Grouping.GroupNotEmptyException.class)
	    public void testDeletingNotEmptyGroup() {
		producer.createGroup(10L);
		producer.insertProfileIntoGroup(new DummyConvertableProfile(), 10L);

		consumer.deleteGroup(10L);
	    }

	    @Test (expected=Grouping.GroupNotFinalException.class)
	    public void  testDeletingNotFinalGroup() {
		producer.createGroup(10L);
		
		consumer.deleteGroup(10L);
	    }

	    @Test (expected=Grouping.NoSuchGroupException.class)
	    public void  testGroupDeletionByDeletingGroupTwice() {
		producer.createGroup(10L);
		producer.markGroupAsFinal(10L);

		consumer.deleteGroup(10L);
		consumer.deleteGroup(10L);
	    }

	    @Test
	    public void testProfilesConsumedInSameOrderAsProduced() {
		producer.createGroup(7L);
		ConvertableProfile[] producedProfiles = new ConvertableProfile[10];
		for (int i = 0; i < producedProfiles.length; i++) {
		    producedProfiles[i] = new DummyConvertableProfile();
		    producer.insertProfileIntoGroup(producedProfiles[i], 7L);
		}

		ConvertableProfile[] consumedProfiles = new ConvertableProfile[10];
		for (int i = 0; consumer.hasNextInGroup(7L); i++)
		    consumedProfiles[i] = consumer.getNextFromGroup(7L);

		assertThat(Arrays.equals(producedProfiles, consumedProfiles), equalTo(true));
	    }
	}
    }
}

/*  
    @Test
    public void testConsumptionOfPopulatedGroup() {
	long groupId = 5;
	ConvertableProfile producedProfile = profileFactory.createMinimalProfile();
	producer.createGroup(groupId);
	producer.insertProfileIntoGroup(producedProfile, groupId);

	ConvertableProfile consumedProfile = consumer.getNextFromGroup(groupId);

	assertThat(consumedProfile, is(sameInstance(producedProfile)));
    }
    */
