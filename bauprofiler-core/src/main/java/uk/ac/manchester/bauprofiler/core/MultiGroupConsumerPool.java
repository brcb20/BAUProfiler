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

import static uk.ac.manchester.bauprofiler.core.MultiGroupConsumerState.State.*;

import java.util.concurrent.atomic.AtomicInteger;

public class MultiGroupConsumerPool implements Distributor, Terminator {

    private static final Integer DEAD_CONSUMERS = null;
    private static final String CNAME = "MultiGroupConsumer";

    private final AtomicInteger totalActivelyConsumedGroups = new AtomicInteger();
    private final MultiGroupConsumerPrototype prototype;
    private final SchedulingPolicy scheduler;
    private final int numOfConsumers;

    private boolean terminated = false;
    private MultiGroupConsumer[] multiGroupConsumers;
    private MultiGroupConsumerState[] mgcThreadStates;
    private GroupConsumerRunnable[] gcrunnables;
    private Integer selectedConsumer;

    public MultiGroupConsumerPool(
            MultiGroupConsumerPrototype prototype
            , SchedulingPolicy scheduler
            , int numOfConsumers) {
        this.prototype = prototype;
        this.scheduler = scheduler;
        this.numOfConsumers = numOfConsumers;
    }

    public void prepareDistribChannel() {
        createMultiGroupConsumersWithStateAndRunnables();
    }

    private void createMultiGroupConsumersWithStateAndRunnables() {
        createMultiGroupConsumerStates();
        createMultiGroupConsumers();
        createGroupConsumerRunnables();
    }

    private void createMultiGroupConsumerStates() {
        mgcThreadStates = new MultiGroupConsumerState[numOfConsumers];
        for (int i = 0; i < numOfConsumers; i++)
            mgcThreadStates[i] = new MultiGroupConsumerState();
    }

    private void createMultiGroupConsumers() {
        multiGroupConsumers = new MultiGroupConsumer[numOfConsumers];
        for (int i = 0; i < numOfConsumers; i++)
            multiGroupConsumers[i] = prototype.produce(this, mgcThreadStates[i]);
    }

    private void createGroupConsumerRunnables() {
        gcrunnables = new GroupConsumerRunnable[numOfConsumers];
        for (int i = 0; i < numOfConsumers; i++)
            gcrunnables[i] = new GroupConsumerRunnableImpl(multiGroupConsumers[i]);
    }

    public void distributeGroup(Long groupId) {
        totalActivelyConsumedGroups.incrementAndGet();
        scheduleGroup(groupId);
    }

    private synchronized void scheduleGroup(Long groupId) {
        if (terminated)
            resurrectConsumers();
        selectConsumer();
        if (allConsumersAreDead())
            throw new DistributionFailure();
        if (isSelectedConsumerNew())
            startConsumerThread();
        distributeGroupToSelectedConsumer(groupId);
    }

    private void resurrectConsumers() {
        for (int i = 0; i < numOfConsumers; i++) {
            gcrunnables[i].resurrect();
            mgcThreadStates[i].set(NEW);
        }
        terminated = false;
    }

    private void selectConsumer() {
        selectedConsumer = scheduler.selectConsumer(multiGroupConsumers);
    }

    private boolean allConsumersAreDead() {
        return selectedConsumer == DEAD_CONSUMERS;
    }

    private boolean isSelectedConsumerNew() {
        return mgcThreadStates[selectedConsumer].get() == NEW;
    }

    private void startConsumerThread() {
        mgcThreadStates[selectedConsumer].set(RUNNING);
        (new Thread(gcrunnables[selectedConsumer], CNAME+selectedConsumer)).start();
    }

    private void distributeGroupToSelectedConsumer(Long groupId) {
        multiGroupConsumers[selectedConsumer].consume(groupId);
    }

    public void notifyGroupConsumed() {
        totalActivelyConsumedGroups.decrementAndGet();
    }

    public boolean isIdle() {
        return totalActivelyConsumedGroups.get() == 0;
    }

    public void tentativeTerminate() {
        if (isIdle())
            terminate();
    }

    private synchronized void terminate() {
        for (int i = 0; i < numOfConsumers; i++) {
            mgcThreadStates[i].set(TERMINATED);
            gcrunnables[i].requestTermination();
            multiGroupConsumers[i].notifyOfTermination();
        }
        terminated = true;
    }

    public static class DistributionFailure extends RuntimeException {
    }
}
