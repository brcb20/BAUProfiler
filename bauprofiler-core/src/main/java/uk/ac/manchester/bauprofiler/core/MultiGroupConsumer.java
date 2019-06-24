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

import java.util.List;
import java.util.ArrayList;

import uk.ac.manchester.bauprofiler.core.converter.ConvertableProfile;
import uk.ac.manchester.bauprofiler.core.converter.Conversion;
import uk.ac.manchester.bauprofiler.core.assembler.Assembler;
import uk.ac.manchester.bauprofiler.core.assembler.Assembly;
import uk.ac.manchester.bauprofiler.core.assembler.AssemblerFactory;

public class MultiGroupConsumer {
    private final ProfileQueueBuilderSelector pqselector = new ProfileQueueBuilderSelector();
    private final GroupConnections connectedGroupIds = new GroupConnections();
    private final Object zeroConnectionLock = new Object();

    private final AssemblerFactory assemblerFactory;
    private final GroupConsumer grouping;
    private final Terminator terminator;
    private final MultiGroupConsumerState state;
    private final ProfilerPrinter printer;

    private Long selectedGroupId = null;

    public MultiGroupConsumer(
            Terminator terminator, GroupConsumer grouping
            , AssemblerFactory assemblerFactory, MultiGroupConsumerState state
            , ProfilerPrinter printer) {
        this.terminator = terminator;
        this.grouping = grouping;
        this.assemblerFactory = assemblerFactory;
        this.state = state;
        this.printer = printer;
    }

    public void execute() {
        checkForAvailableTaskOrWait();
        if (isTimeToTerminate())
            return;
        handleTask();
    }

    private void checkForAvailableTaskOrWait() {
        synchronized (zeroConnectionLock) {
            while (getNumOfConnections() == 0 && state.compareAndSet(RUNNING, WAIT)) {
                waitForAvailableTask();
                state.compareAndSet(WAIT, RUNNING);
            }
        }
    }

    private void waitForAvailableTask() {
        try {
            zeroConnectionLock.wait();
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private boolean isTimeToTerminate() {
        return getNumOfConnections() == 0 && state.get() == TERMINATED;
    }

    private void handleTask() {
        if (hasNotSelectedGroup())
            selectGroup();
        validateAndOrderSelectedGroup();
        if (selectedGroupIsReadyToOutput()) {
            outputSelectedGroup();
            deleteSelectedGroup();
            notifyTerminator();
        }
        if (hasAnotherSelectableGroup())
            selectGroup();
    }

    private boolean hasNotSelectedGroup() {
        return selectedGroupId == null;
    }

    private boolean hasAnotherSelectableGroup() {
        return selectedGroupId != null && getNumOfConnections() > 1;
    }

    private void selectGroup() {
        selectedGroupId = connectedGroupIds.serviceConnection();
        pqselector.select(selectedGroupId);
    }

    private void validateAndOrderSelectedGroup() {
        while (grouping.hasNextInGroup(selectedGroupId)) {
            ConvertableProfile profile = grouping.getNextFromGroup(selectedGroupId);
            if (profile.invalidate())
                continue;
            pqselector.insertIntoSelected(profile);
        }
    }

    private boolean selectedGroupIsReadyToOutput() {
        return grouping.isMarkedAsFinalGroup(selectedGroupId)
            && !grouping.hasNextInGroup(selectedGroupId);
    }

    private void outputSelectedGroup() {
        printer.print(generateOutputForSelectedGroup());
    }

    private Assembly generateOutputForSelectedGroup() {
        return new OutputGenerator(getProfilesForSelectedGroup()).generate();
    }

    private List<ConvertableProfile> getProfilesForSelectedGroup() {
        return pqselector.buildSelected().getProfiles();
    }

    private void deleteSelectedGroup() {
        grouping.deleteGroup(selectedGroupId);
        connectedGroupIds.disconnect();
        pqselector.deleteSelectedReference();
        selectedGroupId = null;
    }

    private void notifyTerminator() {
        terminator.notifyGroupConsumed();
        if (terminator.isIdle())
            terminator.tentativeTerminate();
    }

    public void consume(Long groupId) {
        insertGroupId(groupId);
        if (state.compareAndSet(WAIT, RUNNING))
            wakeUp();
    }

    private void insertGroupId(Long groupId) {
        pqselector.createReference(groupId);
        connectedGroupIds.connect(groupId);
    }

    private void wakeUp() {
        synchronized (zeroConnectionLock) {
            zeroConnectionLock.notify();
        }
    }

    public void notifyOfTermination() {
        wakeUp();
    }

    public boolean isAlive() {
        return state.get() != TERMINATED;
    }

    public int getNumOfConnections() {
        return connectedGroupIds.getNumOfConnections();
    }

    private class OutputGenerator {
        private int conversionSize;
        private List<ConvertableProfile> profiles;
        private List<Conversion> convertedProfiles;
        private Assembly assembly;

        public OutputGenerator(List<ConvertableProfile> profiles) {
            this.profiles = profiles;
        }

        public Assembly generate() {
            postProcessProfiles();
            convertProfiles();
            calculateConversionSize();
            assembleConvertedProfiles();
            return assembly;
        }

        private void postProcessProfiles() {
            for (ConvertableProfile cp : profiles)
                cp.postProcess();
        }

        private void convertProfiles() {
            convertedProfiles = new ArrayList<>(profiles.size());
            for (ConvertableProfile cp : profiles)
                convertedProfiles.add(cp.convert());
        }

        private void calculateConversionSize() {
            conversionSize = 0;
            for (Conversion c : convertedProfiles)
                conversionSize += c.toString().length();
        }

        private void assembleConvertedProfiles() {
            assembly = assemblerFactory
                .create()
                .assemble(convertedProfiles.iterator(), conversionSize);
        }
    }
}
