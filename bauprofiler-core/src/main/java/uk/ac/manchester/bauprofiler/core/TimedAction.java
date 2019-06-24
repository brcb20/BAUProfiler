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

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TimedAction implements Timer, TimerInfo {

    private static double elapsedTimeInMilliSeconds(long start, long end) {
        return BigDecimal.valueOf((end - start)*1e-6)
                         .setScale(5, RoundingMode.HALF_UP)
                         .doubleValue();
    }

    private static double elapsedTimeInMilliSeconds(long time) {
        return BigDecimal.valueOf(time*1e-6)
                         .setScale(5, RoundingMode.HALF_UP)
                         .doubleValue();
    }

    private long start;
    private long end;

    private long currentTime() {
        return System.nanoTime();
    }

    protected TimedAction() {};

    public void start() {
        start = currentTime();
    }

    public void stop() {
        end = currentTime();
    }

    public long getStartTime() {
        return start;
    }

    public double getStartTimeInMilliSeconds() {
        return elapsedTimeInMilliSeconds(start);
    }

    public long getEndTime() {
        return end;
    }

    public double getEndTimeInMilliSeconds() {
        return elapsedTimeInMilliSeconds(end);
    }

    public long getTotalTime() {
        return (end - start);
    }

    public double getTotalTimeInMilliSeconds() {
        return elapsedTimeInMilliSeconds(start, end);
    }
}
