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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;

public class UniqueEncoder {
    private final ConcurrentHashMap<Long, Long> encoding = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> reverseEncoding = new ConcurrentHashMap<>();
    private final AtomicLong uniquePointer;
    private final Long min, max;
    private final int capacity;
    private final LongUnaryOperator loopAroundIncrementByOne;

    public static UniqueEncoder boundedEncoder(int min, int max) {
        if (min < 0 || min > max)
            throw new BoundaryException();
        return new UniqueEncoder(min, max);
    }

    public static UniqueEncoder unboundedEncoder() {
        return boundedEncoder(0, Integer.MAX_VALUE);
    }

    private UniqueEncoder(int min, int max) {
        this.min = (long)min;
        this.max = (long)max;
        capacity = (max-min)+1;
        uniquePointer = new AtomicLong(this.min);
        loopAroundIncrementByOne = (uniqueId) -> (uniqueId == max) ? min : uniqueId+1;
    }

    public void encode(Long groupId) {
        Long uniqueId = tryFindNextAvailableUniqueId(groupId);

        if (encoding.putIfAbsent(groupId, uniqueId) != null) {
            freeUniqueId(uniqueId);
            throw new ReencodingException(groupId);
        }
    }

    private Long tryFindNextAvailableUniqueId(Long groupId) {
        Long uniqueId;
        do {
            if (reverseEncoding.size() == capacity)
                throw new FullCapacityException();
            uniqueId = uniquePointer.getAndUpdate(loopAroundIncrementByOne);
        } while (reverseEncoding.putIfAbsent(uniqueId, groupId) != null);
        return uniqueId;
    }

    private void freeUniqueId(Long uniqueId) {
        reverseEncoding.remove(uniqueId);
    }

    public Long getEncoding(Long groupId) {
        Long uniqueId = encoding.get(groupId);
        if (uniqueId == null)
            throw new NoSuchEncodingException(groupId);
        return uniqueId;
    }

    public void remove(Long groupId) {
        Long uniqueId = encoding.remove(groupId);
        if (uniqueId == null)
            throw new NoSuchEncodingException(groupId);
        reverseEncoding.remove(uniqueId);
    }

    public static class EncodingException extends RuntimeException {
        public EncodingException(long groupId) {
            super(String.format("Caused by GroupId %d", groupId));
        }
    }

    public static class ReencodingException extends EncodingException {
        public ReencodingException(long groupId) {
            super(groupId);
        }
    }

    public static class NoSuchEncodingException extends EncodingException {
        public NoSuchEncodingException(long groupId) {
            super(groupId);
        }
    }

    public static class BoundaryException extends RuntimeException {
    }

    public static class FullCapacityException extends RuntimeException {
    }
}
