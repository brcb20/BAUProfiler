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

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringJoiner;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.CodeBlock;

import uk.ac.manchester.bauprofiler.json.core.*;
import uk.ac.manchester.bauprofiler.core.converter.Conversion;

public class BodyGenerator {
    private JsonContainer container;
    private Encoder encoder;

    public BodyGenerator(JsonContainer container, Encoder encoder) {
	this.container = container;
	this.encoder = encoder;
    }

    public FieldSpec[] getFields() {
	List<FieldSpec> fields = new ArrayList<>();
	fields.add(createAssemblyNodesField());
	fields.add(createIncludeInvisibleField());
	fields.add(createExcludeInvisibleField());
	fields.add(createVerboseField());
	return fields.toArray(new FieldSpec[fields.size()]);
    }

    private FieldSpec createAssemblyNodesField() {
        return FieldSpec.builder(ArrayTypeName.of(ClassName.get(
			"uk.ac.manchester.bauprofiler.core.assembler"
			, "AssemblyNode"))
		, "_assemblyNodes")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer(getAssemblyNodesFormat(), getAssemblyNodesArgs())
            .build();
    }

    private String getAssemblyNodesFormat() {
	StringJoiner format = new StringJoiner(", ", "{", "}");
	for (int i = 0; i < container.prefixes.length; i++)
	    format.add("new $T($S, $L)");
	if (container.childNode.isPresent())
	    format.add("new $T(new $T($S, $L))");
	return format.toString();
    }

    private Object[] getAssemblyNodesArgs() {
	List<Object> args = new ArrayList<>();
	int numOfPrefixes = container.prefixes.length;
	for (int i = 0; i < numOfPrefixes-1; i++)
	    args.addAll(getAssemblyNodeArgs(JType.OBJECT, i));
	if (numOfPrefixes > 0)
	    args.addAll(getAssemblyNodeArgs(container.type, numOfPrefixes-1));
	if (container.childNode.isPresent())
	    args.addAll(getChildAssemblyNodeArgs());
	return args.toArray();
    }

    private List<Object> getAssemblyNodeArgs(JType type, int nodeIndex) {
	return Arrays.asList(
		getNodeClassForType(type)
		, escape(container.prefixes[nodeIndex])
		, encoder.encodePrefix(container.prefixes[nodeIndex]));
    }

    private String escape(String unescaped) {
	return CodeBlock.of("$S", unescaped).toString();
    }

    private List<Object> getChildAssemblyNodeArgs() {
	BaseNode childNode = container.childNode.get();
	return Arrays.asList(
		ChildNode.class
		, childNode.getClass()
		, ""
		, encoder.encodePrefix(childNode.prefix()));
    }

    private Class<? extends BaseNode> getNodeClassForType(JType type) {
	return type.getNodeClass();
    }

    private FieldSpec createIncludeInvisibleField() {
	return createInvisibleField(
		"_includeInvisible", container.fields, container.childFields);
    }

    private FieldSpec createInvisibleField(String name, Field[] fields, Field[] childFields) {
        return FieldSpec.builder(String.class, name)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer("$S", composeIncludeInvisibleField(fields, childFields))
            .build();
    }

    private String composeIncludeInvisibleField(Field[] fields, Field[] childFields) {
	return CodeBlock.of(
		join(
		    getInvisibleFormat(fields.length)
		    , getInvisibleChildFormat(childFields.length))
		, concatenate(
		    getInvisibleArgs(fields)
		    , getInvisibleChildArgs(childFields))
		).toString();
    }

    private String join(String a, String b) {
	if (a.isEmpty())
	    return b;
	if (b.isEmpty())
	    return a;
	return a+","+b;
    }

    private String getInvisibleFormat(int size) {
	StringJoiner format = new StringJoiner(",");
	for (int i = 0; i < size; i++)
	    format.add("$S:$L");
	return format.toString();
    }

    private String getInvisibleChildFormat(int size) {
	return (container.childNode.isPresent()) ? buildInvisibleChildFormat(size) : "";
    }

    private String buildInvisibleChildFormat(int size) {
	BaseNode childNode = container.childNode.get();
	return "$S"+childNode.separator()+childNode.preOpen()+childNode.open()
	    +getInvisibleFormat(size);
    }

    private Object[] concatenate(Object[] a, Object[] b) {
	Object[] concatenation = new Object[a.length+b.length];
	System.arraycopy(a, 0, concatenation, 0, a.length);
	System.arraycopy(b, 0, concatenation, a.length, b.length);
	return concatenation;
    }

    private Object[] getInvisibleArgs(Field[] fields) {
	return getInvisibleArgsFrom(fields);
    }

    private Object[] getInvisibleArgsFrom(Field[] fields) {
	List<Object> args = new ArrayList<>();
	for (Field field : fields)
	    args.addAll(Arrays.asList(field.key, field.valueFormat));
	return args.toArray();
    }

    private Object[] getInvisibleChildArgs(Field[] fields) {
	return concatenate(getChildPrefix(), getInvisibleArgsFrom(fields));
    }

    private Object[] getChildPrefix() {
	return (container.childNode.isPresent())
	    ? new Object[] {container.childNode.get().prefix()}
	    : new Object[0];
    }

    private FieldSpec createExcludeInvisibleField() {
	return createInvisibleField(
		"_excludeInvisible"
		, filterInvisible(container.fields, container.invisibleFields)
		, filterInvisible(container.childFields, container.invisibleChildFields));
    }

    private Field[] filterInvisible(Field[] fields, int invisibles) {
	Field[] filtered = new Field[fields.length-invisibles];
	for (int i = 0, j = 0; i < fields.length; i++) {
	    if (!fields[i].isInvisible)
		filtered[j++] = fields[i];
	    if (j >= filtered.length)
		break;
	}
	return filtered;
    }

    private FieldSpec createVerboseField() {
        return FieldSpec.builder(boolean.class, "_verbose")
            .addModifiers(Modifier.PRIVATE)
            .initializer("$L", "false")
            .build();
    }

    public MethodSpec[] getMethods() {
	List<MethodSpec> methods = new ArrayList<>();
	methods.add(createGetJsonIncludeInvisible());
	methods.add(createGetJsonExcludeInvisible());
	methods.add(createGetJson());
	methods.add(createSetVerbosityMethod());
	methods.add(createConvertMethod());
	return methods.toArray(new MethodSpec[methods.size()]);
    }

    private MethodSpec createGetJsonIncludeInvisible() {
	return createGetJsonInvisible(
		"_includeInvisible", container.fields, container.childFields);
    }

    private MethodSpec createGetJsonInvisible(
	    String format, Field[] fields, Field[] childFields) {
	return MethodSpec.methodBuilder(buildGetJsonMethodName(format))
	    .addModifiers(Modifier.PRIVATE)
	    .returns(String.class)
	    .addStatement(buildGetJsonInvisibleStatement(format, fields, childFields))
	    .build();
    }

    private String buildGetJsonMethodName(String format) {
	return "getJson"+format.substring(1,2).toUpperCase()+format.substring(2);
    }

    private String buildGetJsonInvisibleStatement(
	    String format, Field[] fields, Field[] childFields) {
	StringJoiner sj = new StringJoiner(", ");
	for (Field field : fields)
	    sj.add(field.valueFormatArg);
	for (Field field : childFields)
	    sj.add(field.valueFormatArg);
	return "return String.format("+format+", "+sj.toString()+")";
    }

    private MethodSpec createGetJsonExcludeInvisible() {
	return createGetJsonInvisible(
		"_excludeInvisible"
		, filterInvisible(container.fields, container.invisibleFields)
		, filterInvisible(container.childFields, container.invisibleChildFields));
    }

    private MethodSpec createGetJson() {
	return MethodSpec.methodBuilder("getJson")
	    .addModifiers(Modifier.PRIVATE)
	    .returns(String.class)
	    .addStatement(buildGetJsonStatement())
	    .build();
    }

    private String buildGetJsonStatement() {
	return "return (_verbose) ? getJsonIncludeInvisible() : getJsonExcludeInvisible()";
    }

    private MethodSpec createSetVerbosityMethod() {
	return MethodSpec.methodBuilder("setVerbosity")
	    .addModifiers(Modifier.PUBLIC)
	    .addParameter(TypeName.BOOLEAN, "verbose")
	    .returns(TypeName.VOID)
	    .addStatement("this._verbose = verbose")
	    .build();
    }

    private MethodSpec createConvertMethod() {
	return MethodSpec.methodBuilder("convert")
	    .addModifiers(Modifier.PUBLIC)
	    .returns(Conversion.class)
	    .addStatement(getConvertStatementFormat(), getConvertStatementArgs())
	    .build();
    }

    private String getConvertStatementFormat() {
	return "return new $T("+encoder.nextConversionID()+", getJson(), _assemblyNodes)";
    }

    private Object[] getConvertStatementArgs() {
	return new Object[] {JsonConversion.class};
    }
}
