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
package uk.ac.manchester.bauprofiler.json.generator;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

import java.util.Optional;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.CodeBlock;

import uk.ac.manchester.bauprofiler.json.core.*;

public class BodyGeneratorTest {
    private static final String CORE_PACKAGE = "uk.ac.manchester.bauprofiler.core";
    private static final String FULLY_QUALIFIED_NAME_ASSEMBLYNODE = 
	CORE_PACKAGE+".assembler.AssemblyNode";
    private static final String FULLY_QUALIFIED_NAME_CONVERSION = 
	CORE_PACKAGE+".converter.Conversion";
    private static final String JSON_CORE_PACKAGE = "uk.ac.manchester.bauprofiler.json.core";
    private static final String JSON_GENERATOR_PACKAGE =
	"uk.ac.manchester.bauprofiler.json.generator";

    private JsonContainer container;

    private Optional<FieldSpec> filterFieldsByName(FieldSpec[] fields, String name) {
	for (FieldSpec field : fields)
	    if (field.name.equals(name))
		return Optional.of(field);
	return Optional.empty();
    }

    private Optional<MethodSpec> filterMethodsByName(MethodSpec[] methods, String name) {
	for (MethodSpec method : methods)
	    if (method.name.equals(name))
		return Optional.of(method);
	return Optional.empty();
    }

    private String escape(String unescaped) {
	return CodeBlock.of("$S", unescaped).toString();
    }

    private Field buildInvisibleField(String key, String valueFormat, String valueFormatArg) {
	Field field = buildField(key, valueFormat, valueFormatArg);
	field.isInvisible = true;
	return field;
    }

    private Field buildField(String key, String valueFormat, String valueFormatArg) {
	Field field = new Field();
	field.key = key;
	field.valueFormat = valueFormat;
	field.valueFormatArg = valueFormatArg;
	return field;
    }

    @Before
    public void setupBasicJsonContainer() {
	container = new JsonContainer();
	container.prefixes = new String[0];
	container.fields = new Field[0];
	container.childNode = Optional.empty();
	container.childFields = new Field[0];
    }

    @Test
    public void testAssemblyNodeGen_ForEmptyPrefixes() {
	BodyGenerator bodyGen = new BodyGenerator(container, new DummyEncoder());

	Optional<FieldSpec> assemblyNodes = filterFieldsByName(bodyGen.getFields(), "assemblyNodes");

	assertTrue(assemblyNodes.isPresent());
	assertThat(assemblyNodes.get().toString(), equalTo(
		    "private final "
		    +FULLY_QUALIFIED_NAME_ASSEMBLYNODE
		    +"[] assemblyNodes = {};\n"));
    }

    @Test
    public void testAssemblyNodeGen_ForObjectPrefix() {
	container.prefixes = new String[] {"object"};
	container.type = JType.OBJECT;
	BodyGenerator bodyGen = new BodyGenerator(container, new DummyEncoder());

	Optional<FieldSpec> assemblyNodes = filterFieldsByName(bodyGen.getFields(), "assemblyNodes");

	assertTrue(assemblyNodes.isPresent());
	assertThat(assemblyNodes.get().toString(), equalTo(
		    "private final "
		    +FULLY_QUALIFIED_NAME_ASSEMBLYNODE
		    +"[] assemblyNodes = {new "
		    +JSON_CORE_PACKAGE
		    +".ObjectNode(\"object\", 0)};\n"));
    }

    @Test
    public void testAssemblyNodeGen_ForArrayObjectPrefix() {
	container.prefixes = new String[] {"objectArray"};
	container.type = JType.OBJECT_ARRAY;
	BodyGenerator bodyGen = new BodyGenerator(container, new DummyEncoder());

	Optional<FieldSpec> assemblyNodes = filterFieldsByName(bodyGen.getFields(), "assemblyNodes");

	assertTrue(assemblyNodes.isPresent());
	assertThat(assemblyNodes.get().toString(), equalTo(
		    "private final "
		    +FULLY_QUALIFIED_NAME_ASSEMBLYNODE
		    +"[] assemblyNodes = {new "
		    +JSON_CORE_PACKAGE
		    +".ObjectArrayNode(\"objectArray\", 0)};\n"));
    }

    @Test
    public void testAssemblyNodeGen_ForObjectPrefix_WithChildArrayNode() {
	container.prefixes = new String[] {"object"};
	container.type = JType.OBJECT;
	container.childNode = Optional.of(new ArrayNode("childArray", 0));
	BodyGenerator bodyGen = new BodyGenerator(container, new DummyEncoder());

	Optional<FieldSpec> assemblyNodes = filterFieldsByName(bodyGen.getFields(), "assemblyNodes");

	assertTrue(assemblyNodes.isPresent());
	assertThat(assemblyNodes.get().toString(), equalTo(
		    "private final "
		    +FULLY_QUALIFIED_NAME_ASSEMBLYNODE
		    +"[] assemblyNodes = "
		    +"{new "+JSON_CORE_PACKAGE+".ObjectNode(\"object\", 0)"
		    +", new "+JSON_CORE_PACKAGE+".ChildNode("
		    +"new "+JSON_CORE_PACKAGE+".ArrayNode(\"childArray\", 0))};\n"));
    }

    @Test
    public void testJsonFormattingGen_includeInvisible_noChild() {
	container.fields = new Field[] {
	    buildInvisibleField("name", "\"%s\"", "name")
	    , buildField("secondName", "\"%s\"", "secondName")
	};
	container.invisibleFields = 1;
	BodyGenerator bodyGen = new BodyGenerator(container, new DummyEncoder());

	Optional<FieldSpec> includeInvisible =
	    filterFieldsByName(bodyGen.getFields(), "includeInvisible");

	assertTrue(includeInvisible.isPresent());
	assertThat(includeInvisible.get().toString(), equalTo(
		    "private final java.lang.String includeInvisible = "
		    +escape("\"name\":\"%s\",\"secondName\":\"%s\"")
		    +";\n"));
    }

    @Test
    public void testJsonFormattingGen_includeInvisible_withObjectChild() {
	container.childNode = Optional.of(new ObjectNode("child", 0));
	container.childFields = new Field[] {
	    buildField("childName", "\"%s\"", "childName")
	    , buildInvisibleField("secondChildName", "\"%s\"", "secondChildName")
	};
	container.invisibleChildFields = 1;
	BodyGenerator bodyGen = new BodyGenerator(container, new DummyEncoder());

	Optional<FieldSpec> includeInvisible =
	    filterFieldsByName(bodyGen.getFields(), "includeInvisible");

	assertTrue(includeInvisible.isPresent());
	assertThat(includeInvisible.get().toString(), equalTo(
		    "private final java.lang.String includeInvisible = "
		    +escape("\"child\":{\"childName\":\"%s\",\"secondChildName\":\"%s\"")
		    +";\n"));
    }

    @Test
    public void testJsonFormattingGen_excludeInvisible_noChild() {
	container.fields = new Field[] {
	    buildField("name", "\"%s\"", "name")
	    , buildInvisibleField("secondName", "\"%s\"", "secondName")
	};
	container.invisibleFields = 1;
	BodyGenerator bodyGen = new BodyGenerator(container, new DummyEncoder());

	Optional<FieldSpec> includeInvisible =
	    filterFieldsByName(bodyGen.getFields(), "excludeInvisible");

	assertTrue(includeInvisible.isPresent());
	assertThat(includeInvisible.get().toString(), equalTo(
		    "private final java.lang.String excludeInvisible = "
		    +escape("\"name\":\"%s\"")
		    +";\n"));
    }

    @Test
    public void testJsonFormattingGen_excludeInvisible_withChild() {
	container.fields = new Field[] {
	    buildField("name", "\"%s\"", "name")
	};
	container.childNode = Optional.of(new ObjectNode("child", 0));
	container.childFields = new Field[] {
	    buildField("childName", "\"%s\"", "childName")
	    , buildInvisibleField("nextChildName", "\"%s\"", "nextChildName")
	};
	container.invisibleChildFields = 1;
	BodyGenerator bodyGen = new BodyGenerator(container, new DummyEncoder());

	Optional<FieldSpec> includeInvisible =
	    filterFieldsByName(bodyGen.getFields(), "excludeInvisible");

	assertTrue(includeInvisible.isPresent());
	assertThat(includeInvisible.get().toString(), equalTo(
		    "private final java.lang.String excludeInvisible = "
		    +escape("\"name\":\"%s\",\"child\":{\"childName\":\"%s\"")
		    +";\n"));
    }

    @Test
    public void testGetJsonIncludeInvisible_noChild() {
	container.fields = new Field[] {
	    buildField("name", "\"%s\"", "name")
	    , buildInvisibleField("nextName", "\"%s\"", "nextName")
	};
	container.invisibleFields = 1;

	BodyGenerator bodyGen = new BodyGenerator(container, new DummyEncoder());

	Optional<MethodSpec> getJsonIncludeInvisible =
	    filterMethodsByName(bodyGen.getMethods(), "getJsonIncludeInvisible");

	assertTrue(getJsonIncludeInvisible.isPresent());
	assertThat(getJsonIncludeInvisible.get().toString(), equalTo(
		    "private java.lang.String getJsonIncludeInvisible() {\n"
		    + "  return String.format(includeInvisible, name, nextName);\n"
		    + "}\n"));
    }

    @Test
    public void testGetJsonIncludeInvisible_withChild() {
	container.fields = new Field[] {
	    buildField("name", "\"%s\"", "name")
	};
	container.childNode = Optional.of(new ObjectNode("child", 0));
	container.childFields = new Field[] {
	    buildField("childName", "\"%s\"", "childName")
	    , buildInvisibleField("nextChildName", "\"%s\"", "nextChildName")
	};
	container.invisibleChildFields = 1;

	BodyGenerator bodyGen = new BodyGenerator(container, new DummyEncoder());

	Optional<MethodSpec> getJsonIncludeInvisible =
	    filterMethodsByName(bodyGen.getMethods(), "getJsonIncludeInvisible");

	assertTrue(getJsonIncludeInvisible.isPresent());
	assertThat(getJsonIncludeInvisible.get().toString(), equalTo(
		    "private java.lang.String getJsonIncludeInvisible() {\n"
		    + "  return String.format("
		    + "includeInvisible, name, childName, nextChildName);\n"
		    + "}\n"));
    }

    @Test
    public void testGetJsonExcludeInvisible_noChild() {
	container.fields = new Field[] {
	    buildField("name", "\"%s\"", "name")
	    , buildInvisibleField("nextName", "\"%s\"", "nextName")
	};
	container.invisibleFields = 1;

	BodyGenerator bodyGen = new BodyGenerator(container, new DummyEncoder());

	Optional<MethodSpec> getJsonExcludeInvisible =
	    filterMethodsByName(bodyGen.getMethods(), "getJsonExcludeInvisible");

	assertTrue(getJsonExcludeInvisible.isPresent());
	assertThat(getJsonExcludeInvisible.get().toString(), equalTo(
		    "private java.lang.String getJsonExcludeInvisible() {\n"
		    + "  return String.format(excludeInvisible, name);\n"
		    + "}\n"));
    }

    @Test
    public void testGetJsonExcludeInvisible_withChild() {
	container.fields = new Field[] {
	    buildField("name", "\"%s\"", "name")
	};
	container.childNode = Optional.of(new ObjectNode("child", 0));
	container.childFields = new Field[] {
	    buildField("childName", "\"%s\"", "childName")
	    , buildInvisibleField("nextChildName", "\"%s\"", "nextChildName")
	};
	container.invisibleChildFields = 1;

	BodyGenerator bodyGen = new BodyGenerator(container, new DummyEncoder());

	Optional<MethodSpec> getJsonExcludeInvisible =
	    filterMethodsByName(bodyGen.getMethods(), "getJsonExcludeInvisible");

	assertTrue(getJsonExcludeInvisible.isPresent());
	assertThat(getJsonExcludeInvisible.get().toString(), equalTo(
		    "private java.lang.String getJsonExcludeInvisible() {\n"
		    + "  return String.format("
		    + "excludeInvisible, name, childName);\n"
		    + "}\n"));
    }

    @Test
    public void testGetJsonMethod() {
	BodyGenerator bodyGen = new BodyGenerator(container, new DummyEncoder());

	Optional<MethodSpec> getJson =
	    filterMethodsByName(bodyGen.getMethods(), "getJson");

	assertTrue(getJson.isPresent());
	assertThat(getJson.get().toString(), equalTo(
		    "private java.lang.String getJson() {\n"
		    + "  return (verbose) "
		    + "? getJsonIncludeInvisible() : getJsonExcludeInvisible();\n"
		    + "}\n"));
    }

    @Test
    public void testVerboseField() {
	BodyGenerator bodyGen = new BodyGenerator(container, new DummyEncoder());

	Optional<FieldSpec> verbose =
	    filterFieldsByName(bodyGen.getFields(), "verbose");

	assertTrue(verbose.isPresent());
	assertThat(verbose.get().toString(), equalTo("private boolean verbose = false;\n"));
    }

    @Test
    public void testSetVerbosityMethod() {
	BodyGenerator bodyGen = new BodyGenerator(container, new DummyEncoder());

	Optional<MethodSpec> verbosity =
	    filterMethodsByName(bodyGen.getMethods(), "setVerbosity");

	assertTrue(verbosity.isPresent());
	assertThat(verbosity.get().toString(), equalTo(
		    "public void setVerbosity(boolean verbose) {\n"
		    +"  this.verbose = verbose;\n"
		    +"}\n"));
    }

    @Test
    public void testConvertMethod() {
	BodyGenerator bodyGen = new BodyGenerator(container, new DummyEncoder());

	Optional<MethodSpec> convert =
	    filterMethodsByName(bodyGen.getMethods(), "convert");

	assertTrue(convert.isPresent());
	assertThat(convert.get().toString(), equalTo(
		    "public "+FULLY_QUALIFIED_NAME_CONVERSION+" convert() {\n"
		    + "  return new "+JSON_GENERATOR_PACKAGE+".JsonConversion("
		    + "0, getJson(), assemblyNodes);\n"
		    + "}\n"));
    }
}
