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

import java.util.List;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import uk.ac.manchester.bauprofiler.core.assembler.Assembly;

public class MultiGroupConsumerTest {
    private ManuallyPopulatedGroupConsumerStub manualProducer;
    private GroupConsumer consumer;
    private MultiGroupConsumerState state;
    private MultiGroupConsumer multiGroupConsumer;
    private TerminatorSpy terminatorSpy;
    private GroupConsumerRunnableSpy runnableSpy;
    private AssemblerSpyFactoryStub assemblerFactoryStub;
    private Thread testThread;

    private void sleep(int duration) {
	try {
	    Thread.currentThread().sleep(duration);
	} catch (InterruptedException e) {
	    fail(Thread.currentThread().getName()
		    +": interrupted unexpectedly");
	}
    }

    @Before
    public void setup() {
	manualProducer = new ManuallyPopulatedGroupConsumerStub();
	consumer = manualProducer;
	state = new MultiGroupConsumerState();
	terminatorSpy = new TerminatorSpy();
	assemblerFactoryStub = new AssemblerSpyFactoryStub();
	multiGroupConsumer = new MultiGroupConsumer(
		terminatorSpy
		, consumer
		, assemblerFactoryStub
		, state
		, new ProfilerPrinter() {
		    public void print(Assembly output) {
		    }
		});
	runnableSpy = new GroupConsumerRunnableSpy(multiGroupConsumer);

	testThread = new Thread(runnableSpy, "MultiGroupConsumerTestThread");
	state.set(MultiGroupConsumerState.State.RUNNING);
	testThread.start();
    }

    @After
    public void terminate() {
	state.set(MultiGroupConsumerState.State.TERMINATED);
	runnableSpy.requestTermination();
	multiGroupConsumer.notifyOfTermination();

	runnableSpy.releaseFromCtrl(1);
    }

    @Test
    public void testWaitsWhenNoTasksToHandle() {
	runnableSpy.releaseFromCtrl(1);

	sleep(10);
	assertThat(state.get(), equalTo(MultiGroupConsumerState.State.WAIT));
    }

    @Test
    public void testTerminatesWhenRequestedFromWait() {
	runnableSpy.releaseFromCtrl(1);

	terminate();

	sleep(10);
	assertThat(runnableSpy.isAlive(), equalTo(false));
    }


    @Test
    public void testAssemblyOfSingleConsumedGroup() {
	manualProducer.populateGroup(10L, 5);
	manualProducer.markGroupAsFinal(10L);
	terminatorSpy.distributeGroup(10L);
	multiGroupConsumer.consume(10L);

	runnableSpy.releaseFromCtrl(1);

	sleep(10);
	List<AssemblerSpy> assemblerSpies = assemblerFactoryStub.getAssemblers();
	assertThat(assemblerSpies.size(), equalTo(1));
	AssemblerSpy assemblerSpy = assemblerSpies.get(0);
	assertThat(assemblerSpy.hasBeenAssembled(), equalTo(true));
	assertThat(assemblerSpy.getConversionSize(), equalTo(5));
    }

    @Test
    public void testTerminationRequestMade_afterAssemblyOfSingleConsumedGroup() {
	manualProducer.populateGroup(10L, 5);
	manualProducer.markGroupAsFinal(10L);
	terminatorSpy.distributeGroup(10L);
	multiGroupConsumer.consume(10L);

	runnableSpy.releaseFromCtrl(1);

	sleep(10);
	assertThat(terminatorSpy.getTentativeTerminateCount(), equalTo(1));
    }

    @Test
    public void testAfterPartialConsumptionOfGroup_beginsConsumptionOfNextGroup() {
	manualProducer.populateGroup(10L, 4);
	manualProducer.populateGroup(30L, 4);
	terminatorSpy.distributeGroup(10L);
	terminatorSpy.distributeGroup(30L);
	multiGroupConsumer.consume(10L);
	multiGroupConsumer.consume(30L);

	runnableSpy.releaseFromCtrl(1);
	sleep(10);
	assertThat(manualProducer.hasNextInGroup(10L), equalTo(false));

	runnableSpy.releaseFromCtrl(1);
	sleep(10);
	assertThat(manualProducer.hasNextInGroup(30L), equalTo(false));
    }

    @Test
    public void testWakesUp_whenRequestedToConsume() {
	runnableSpy.releaseFromCtrl(1);
	sleep(10);
	assertThat(state.get(), equalTo(MultiGroupConsumerState.State.WAIT));

	manualProducer.populateGroup(10L, 4);
	terminatorSpy.distributeGroup(10L);
	multiGroupConsumer.consume(10L);
	sleep(10);
	assertThat(manualProducer.hasNextInGroup(10L), equalTo(false));
    }
}
