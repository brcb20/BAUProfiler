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
package uk.ac.manchester.bauprofiler.json.assembler;

import java.util.Iterator;
import java.util.Deque;
import java.util.List;
import java.util.ArrayDeque;
import java.util.ArrayList;

import uk.ac.manchester.bauprofiler.core.assembler.Assembler;
import uk.ac.manchester.bauprofiler.core.assembler.AssemblyNode;
import uk.ac.manchester.bauprofiler.core.assembler.Assembly;
import uk.ac.manchester.bauprofiler.core.converter.Conversion;

public class JsonAssembler implements Assembler {

    public Assembly assemble(Iterator<Conversion> conversions, int estimatedSize) {
        return new InternalAssembler(conversions, (int)(estimatedSize*1.02)).assemble();
    }

    private class InternalAssembler {
        private StringBuilder json;
        private Iterator<Conversion> conversions;
        private String body;
        private AssemblyNode[] newDepthTree;
        private List<AssemblyNode> depthTree = new ArrayList<>();
        private Deque<Long> conversionIds = new ArrayDeque<>();
        private Deque<Integer> conversionIdDepths = new ArrayDeque<>();
        private long conversionID;
        private String separator;

        public InternalAssembler(Iterator<Conversion> conversions, int size) {
            this.conversions = conversions;
            json = new StringBuilder(size);
        }

        public Assembly assemble() {
            openObject();
            includeFirstConversion();
            includeOtherConversions();
            closeRemainingLayersOfDepth();
            closeObject();
            return new JsonAssembly(json.toString());
        }

        private void openObject() {
            json.append('{');
        }

        private boolean hasNextConversion() {
            return conversions.hasNext();
        }

        private void includeFirstConversion() {
            if (hasNextConversion()) {
                getNextConversion();
                openNewLayersOfDepth();
                includeConversionBody();
            }
        }

        private void includeOtherConversions() {
            while (hasNextConversion()) {
                getNextConversion();
                closeExcessLayersOfDepth();
                determineSeparator();
                closeMismatchingLayersOfDepth();
                includeSeparator();
                openNewLayersOfDepth();
                includeConversionBody();
            }
        }

        private void getNextConversion() {
            Conversion c = conversions.next();
            body = c.toString();
            newDepthTree = c.getAssemblyNodes();
            conversionID = c.id();
        }

        private void closeMismatchingConversionDepth() {
            closeExcessConversionDepth();
        }

        private void openNewLayersOfDepth() {
            openConversionDepth();
            for (int i = depthTree.size(); i < newDepthTree.length; i++) {
                depthTree.add(newDepthTree[i]);
                json.append(newDepthTree[i].prefix()
                        + newDepthTree[i].separator()
                        + newDepthTree[i].preOpen()
                        + newDepthTree[i].open());
            }
        }

        private void openConversionDepth() {
            if (newDepthTree.length > depthTree.size()) {
                conversionIds.addLast(conversionID);
                conversionIdDepths.addLast(newDepthTree.length);
            }
        }

        private void includeConversionBody() {
            json.append(body);
        }

        private void determineSeparator() {
            if (isMatchingConversion()) {
                AssemblyNode n = depthTree.get(depthTree.size()-1);
                separator = n.close()+","+n.open();
            } else
                separator = ",";
        }

        private boolean isMatchingConversion() {
            return conversionIds.size() > 0
                && conversionIds.peekLast() == conversionID;
        }

        private void includeSeparator() {
            json.append(separator);
        }

        private void closeExcessLayersOfDepth() {
            while (depthTree.size() > newDepthTree.length)
                closeMaxDepth();
            closeExcessConversionDepth();
        }

        private void closeMaxDepth() {
            AssemblyNode deepestNode = depthTree.remove(depthTree.size()-1);
            json.append(deepestNode.close()+deepestNode.postClose());
        }

        private void closeExcessConversionDepth() {
            while (conversionIds.size() > 0
                    && conversionIdDepths.peekLast() > depthTree.size()) {
                conversionIds.removeLast();
                conversionIdDepths.removeLast();
            }
        }

        private void closeMismatchingLayersOfDepth() {
            if (isMatchingConversion())
                return;

            int equalityAtDepth = findMaxDepthEquality();
            for (int i = findMaxDepth(); i > equalityAtDepth; i--)
                closeMaxDepth();

            closeMismatchingConversionDepth();
        }

        private int findMaxDepthEquality() {
            int maxDepth = findMaxDepth();
            for (int equality = 0; equality < maxDepth; equality++)
                if (!isMatchingNodeIDAtDepth(equality))
                    return equality;
            return maxDepth;
        }

        private int findMaxDepth() {
            return depthTree.size();
        }

        private boolean isMatchingNodeIDAtDepth(int depth) {
            return depthTree.get(depth).uniqueId() == newDepthTree[depth].uniqueId();
        }

        private void closeRemainingLayersOfDepth() {
            while (depthTree.size() > 0)
                closeMaxDepth();
        }

        private void closeObject() {
            json.append('}');
        }
    }
}
