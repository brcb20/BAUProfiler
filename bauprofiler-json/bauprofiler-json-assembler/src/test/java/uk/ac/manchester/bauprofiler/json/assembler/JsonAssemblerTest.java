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

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.Test;
import org.junit.Before;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;

import uk.ac.manchester.bauprofiler.core.assembler.AssemblyNode;
import uk.ac.manchester.bauprofiler.core.converter.Conversion;
import uk.ac.manchester.bauprofiler.json.core.ObjectNode;
import uk.ac.manchester.bauprofiler.json.core.ObjectArrayNode;
import uk.ac.manchester.bauprofiler.json.core.ArrayNode;

/* TODO Extremely hard to understand: 
 * Refactor to Assemble(Disassemble(json_string)) 
 * and test assembling with child node */
public class JsonAssemblerTest {
    private static final String OPEN_OBJECT = "{";
    private static final String CLOSE_OBJECT = "}";
    private static final String OPEN_ARRAY = "[";
    private static final String CLOSE_ARRAY = "]";
    private static final String VALUE_SEPARATOR = ",";
    private static final String KEY_VALUE_SEPARATOR = ":";
    private static final String EMPTY_JSON_OBJECT = "{}";
    private static final String EMPTY = "";

    private JsonAssembler assembler;
    private long nextNodeID;
    private Map<String, Long> uniqueNodeIDs;
    private long nextConversionID;

    @Before
    public void setup() {
	assembler = new JsonAssembler();
	nextNodeID = 0;
	uniqueNodeIDs = new HashMap<>();
	nextConversionID = 0;
    }

    private long uniquelyEncodePrefix(String prefix) {
	if (!uniqueNodeIDs.containsKey(prefix))
	    uniqueNodeIDs.put(prefix, nextNodeID++);
	return uniqueNodeIDs.get(prefix);
    }

    private Iterator<Conversion> createZeroDepthConversionIterator(String... bodies) {
	List<Conversion> conversions = new ArrayList<>();
	for (String body : bodies)
	    conversions.add(createZeroDepthConversion(body));
	return conversions.iterator();
    }

    private Conversion createZeroDepthConversion(String body) {
	return createConversion(body, new AssemblyNode[]{}, getNextConversionId());
    }

    private long getNextConversionId() {
	return nextConversionID++;
    }

    private Conversion createConversion(
	    String body, AssemblyNode[] assemblyNodes, long conversionId) {
	return new Conversion() {
	    public long id() {
		return conversionId;
	    }
	    public String toString() {
		return body;
	    }
	    public AssemblyNode[] getAssemblyNodes() {
		return assemblyNodes;
	    }
	};
    }

    private AssemblyNode[] createObjectDepthTree(String... prefixes) {
	List<AssemblyNode> depthTree = new ArrayList<>();
	depthTree = appendObjectsToDepthTree(depthTree, prefixes);
	return depthTree.toArray(new AssemblyNode[depthTree.size()]);
    }

    private List<AssemblyNode> appendObjectsToDepthTree(
	    List<AssemblyNode> depthTree, String... prefixes) {
	long parentEncoding = getMostRecentParentEncoding(depthTree);
	for (String prefix : prefixes) {
	    AssemblyNode n = new ObjectNode(prefix, uniquelyEncodePrefix(prefix));
	    depthTree.add(n);
	    parentEncoding = n.uniqueId();
	}
	return depthTree;
    }

    private long getMostRecentParentEncoding(List<AssemblyNode> depthTree) {
	return ((depthTree.isEmpty())
		? 0
		: depthTree.get(depthTree.size()-1).uniqueId());
    }

    private AssemblyNode[] createArrayDepthTree(String... prefixes) {
	List<AssemblyNode> depthTree = new ArrayList<>();
	depthTree = appendArrayToDepthTree(
		appendObjectsToDepthTree(
		    depthTree
		    , Arrays.copyOfRange(prefixes, 0, prefixes.length-1))
		, prefixes[prefixes.length-1]);
	return depthTree.toArray(new AssemblyNode[depthTree.size()]);
    }

    private List<AssemblyNode> appendArrayToDepthTree(
	    List<AssemblyNode> depthTree, String prefix) {
	depthTree.add(new ArrayNode(prefix, uniquelyEncodePrefix(prefix)));
	return depthTree;
    }

    private AssemblyNode[] createObjectArrayDepthTree(String... prefixes) {
	List<AssemblyNode> depthTree = new ArrayList<>();
	depthTree = appendObjectArrayToDepthTree(
		appendObjectsToDepthTree(
		    depthTree
		    , Arrays.copyOfRange(prefixes, 0, prefixes.length-1))
		, prefixes[prefixes.length-1]);
	return depthTree.toArray(new AssemblyNode[depthTree.size()]);
    }

    private List<AssemblyNode> appendObjectArrayToDepthTree(
	    List<AssemblyNode> depthTree, String prefix) {
	depthTree.add(new ObjectArrayNode(prefix, uniquelyEncodePrefix(prefix)));
	return depthTree;
    }

    @Test
    public void testAssemblingEmptyConversions() {
	String assembly = assembler.assemble(Collections.emptyIterator(), 10).toString();
	assertThat(assembly, equalTo(EMPTY_JSON_OBJECT));
    }

    @Test
    public void testAssemblingSingleKeyValuePair() {
	String body = "\"metric\":\"TORNADO_RUNTIME\"";
	String assembly = assembler
	    .assemble(createZeroDepthConversionIterator(body), 10).toString();

	assertThat(assembly, equalTo(OPEN_OBJECT+body+CLOSE_OBJECT));
    }

    @Test
    public void testAssemblingTwoKeyValuePairs() {
	String firstBody = "\"metric\":\"TORNADO_RUNTIME\"";
	String secondBody = "\"j_totalTime\":228870945";
	String assembly = assembler.assemble(
		createZeroDepthConversionIterator(firstBody, secondBody), 10).toString();

	assertThat(assembly, equalTo(
		    OPEN_OBJECT
		    +firstBody
		    +VALUE_SEPARATOR
		    +secondBody
		    +CLOSE_OBJECT));
    }

    @Test
    public void testAssemblingSingleKeyValuePairWithDepth() {
	String body = "\"metric\":\"TORNADO_RUNTIME\"";
	List<Conversion> conversions = new ArrayList<>();
	AssemblyNode[] depthTree = createObjectDepthTree("\"ProfiledAction\"");
	conversions.add(createConversion(body, depthTree, getNextConversionId()));
	String assembly = assembler.assemble(conversions.iterator(), 10).toString();

	assertThat(assembly, equalTo(
		    OPEN_OBJECT
		    +depthTree[0].prefix()
		    +depthTree[0].separator()
		    +depthTree[0].open()
		    +body
		    +depthTree[0].close()
		    +CLOSE_OBJECT));
    }

    @Test
    public void testAssemblingTwoKeyValuePairsAtEqualDepthAndEqualNodeComposition() {
	String[] values = new String[] {
	    "\"metric\":\"TORNADO_RUNTIME\""
	    , "\"j_totalTime\":228870945"
	};
	AssemblyNode[] firstDepthTree = createObjectDepthTree("\"ProfiledAction\"");
	AssemblyNode[] secondDepthTree = createObjectDepthTree("\"ProfiledAction\"");
	List<Conversion> conversions = new ArrayList<>();
	conversions.add(createConversion(values[0], firstDepthTree, getNextConversionId()));
	conversions.add(createConversion(values[1], secondDepthTree, getNextConversionId()));
	String assembly = assembler.assemble(conversions.iterator(), 10).toString();

	assertThat(assembly, equalTo(
		    OPEN_OBJECT
		    +firstDepthTree[0].prefix()
		    +firstDepthTree[0].separator()
		    +firstDepthTree[0].open()
		    +values[0]
		    +VALUE_SEPARATOR
		    +values[1]
		    +firstDepthTree[0].close()
		    +CLOSE_OBJECT));
    }

    @Test
    public void testAssemblingTwoKeyValuePairsAtEqualDepthAndDiffNodeComposition() {
	String firstBody = "\"metric\":\"TORNADO_RUNTIME\"";
	String secondBody = "\"metric\":\"TORNADO_BUILD_GRAPH\"";
	AssemblyNode[] firstDepthTree = createObjectDepthTree("\"Runtime\"");
	AssemblyNode[] secondDepthTree = createObjectDepthTree("\"Graph\"");
	List<Conversion> conversions = new ArrayList<>();
	conversions.add(createConversion(firstBody, firstDepthTree, getNextConversionId()));
	conversions.add(createConversion(secondBody, secondDepthTree, getNextConversionId()));
	String assembly = assembler.assemble(conversions.iterator(), 10).toString();

	assertThat(assembly, equalTo(
		    OPEN_OBJECT
		    +firstDepthTree[0].prefix()
		    +firstDepthTree[0].separator()
		    +firstDepthTree[0].open()
		    +firstBody
		    +firstDepthTree[0].close()
		    +VALUE_SEPARATOR
		    +secondDepthTree[0].prefix()
		    +secondDepthTree[0].separator()
		    +secondDepthTree[0].open()
		    +secondBody
		    +secondDepthTree[0].close()
		    +CLOSE_OBJECT));
    }

    @Test
    public void testAssemblingArrayOfValues() {
	String[] required = new String[] {"\"id\"", "\"name\"", "\"price\""};
	AssemblyNode[] sharedDepthTree = createArrayDepthTree("\"required\"");
	List<Conversion> conversions = new ArrayList<>();
	long conversionId = getNextConversionId();
	for (String r : required)
	    conversions.add(createConversion(r, sharedDepthTree, conversionId));
	String assembly = assembler.assemble(conversions.iterator(), 10).toString();

	assertThat(assembly, equalTo(
		    OPEN_OBJECT
		    +sharedDepthTree[0].prefix()
		    +sharedDepthTree[0].separator()
		    +sharedDepthTree[0].preOpen()
		    +required[0]
		    +VALUE_SEPARATOR
		    +required[1]
		    +VALUE_SEPARATOR
		    +required[2]
		    +sharedDepthTree[0].postClose()
		    +CLOSE_OBJECT));
    }

    @Test
    public void testAssemblingArrayOfObjects() {
	String[] phoneNumbers = new String[] {
	    "\"type\": \"home\", \"number\": \"212 555-1234\""
	    , "\"type\": \"fax\", \"number\": \"646 555-4567\""
	};
	AssemblyNode[] sharedDepthTree = createObjectArrayDepthTree("\"phoneNumber\"");
	List<Conversion> conversions = new ArrayList<>();
	long conversionId = getNextConversionId();
	for (String pn : phoneNumbers)
	    conversions.add(createConversion(pn, sharedDepthTree, conversionId));
	String assembly = assembler.assemble(conversions.iterator(), 10).toString();

	assertThat(assembly, equalTo(
		    OPEN_OBJECT
		    +sharedDepthTree[0].prefix()
		    +sharedDepthTree[0].separator()
		    +sharedDepthTree[0].preOpen()
		    +sharedDepthTree[0].open()
		    +phoneNumbers[0]
		    +sharedDepthTree[0].close()
		    +VALUE_SEPARATOR
		    +sharedDepthTree[0].open()
		    +phoneNumbers[1]
		    +sharedDepthTree[0].close()
		    +sharedDepthTree[0].postClose()
		    +CLOSE_OBJECT));
    }

    @Test
    public void testAssemblingTwoKeyValuePairsAndObjectFromBaseObject() {
	String baseDepthPrefix = "\"vmbytecode\"";
	String[] keyValuePairs = new String[] {
	    "\"metric\":\"TORNADO_RUNTIME\""
	    , "\"ocl_timing\":6529479"
	    , "\"j_totalTime\":228870945"
	};
	AssemblyNode[] baseDepthTree = createObjectDepthTree(baseDepthPrefix);
	AssemblyNode[] basePlusOneDepthTree =
	    createObjectDepthTree(baseDepthPrefix, "\"events\"");
	List<Conversion> conversions = new ArrayList<>();
	conversions.add(createConversion(
		    keyValuePairs[0], baseDepthTree, getNextConversionId()));
	conversions.add(createConversion(
		    keyValuePairs[1], basePlusOneDepthTree, getNextConversionId()));
	conversions.add(createConversion(
		    keyValuePairs[2], baseDepthTree, getNextConversionId()));
	String assembly = assembler.assemble(conversions.iterator(), 50).toString();

	assertThat(assembly, equalTo(
		    OPEN_OBJECT
		    +baseDepthTree[0].prefix()
		    +baseDepthTree[0].separator()
		    +baseDepthTree[0].open()
		    +keyValuePairs[0]
		    +VALUE_SEPARATOR
		    +basePlusOneDepthTree[1].prefix()
		    +basePlusOneDepthTree[1].separator()
		    +basePlusOneDepthTree[1].open()
		    +keyValuePairs[1]
		    +basePlusOneDepthTree[1].close()
		    +VALUE_SEPARATOR
		    +keyValuePairs[2]
		    +baseDepthTree[0].close()
		    +CLOSE_OBJECT));
    }

    @Test
    public void testAssemblingTwoKeyValuePairsAndObjectTwiceFromBaseObjectArray() {
	String baseDepthPrefix = "\"vmbytecode\"";
	String[] keyValuePairs = new String[] {
	    "\"metric\":\"TORNADO_RUNTIME\""
	    , "\"ocl_timing\":6529479"
	    , "\"j_totalTime\":228870945"
	};
	AssemblyNode[] baseDepthTree = createObjectArrayDepthTree(baseDepthPrefix);
	AssemblyNode[] basePlusOneDepthTree =
	    createObjectDepthTree(baseDepthPrefix, "\"events\"");
	List<Conversion> conversions = new ArrayList<>();
	conversions.add(createConversion(
		    keyValuePairs[0], baseDepthTree, getNextConversionId()));
	conversions.add(createConversion(
		    keyValuePairs[1], basePlusOneDepthTree, getNextConversionId()));
	conversions.add(createConversion(
		    keyValuePairs[2], baseDepthTree, getNextConversionId()));
	conversions.add(conversions.get(0));
	conversions.add(conversions.get(1));
	conversions.add(conversions.get(2));
	String assembly = assembler.assemble(conversions.iterator(), 50).toString();

	String objectBody =
	    keyValuePairs[0]
	    +VALUE_SEPARATOR
	    +basePlusOneDepthTree[1].prefix()
	    +basePlusOneDepthTree[1].separator()
	    +basePlusOneDepthTree[1].open()
	    +keyValuePairs[1]
	    +basePlusOneDepthTree[1].close()
	    +VALUE_SEPARATOR
	    +keyValuePairs[2];

	assertThat(assembly, equalTo(
		    OPEN_OBJECT
		    +baseDepthTree[0].prefix()
		    +baseDepthTree[0].separator()
		    +baseDepthTree[0].preOpen()
		    +baseDepthTree[0].open()
		    +objectBody
		    +baseDepthTree[0].close()
		    +VALUE_SEPARATOR
		    +baseDepthTree[0].open()
		    +objectBody
		    +baseDepthTree[0].close()
		    +baseDepthTree[0].postClose()
		    +CLOSE_OBJECT));
    }
}
