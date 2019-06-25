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
import java.util.StringJoiner;
import java.time.LocalDateTime;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.Modifier;
import javax.annotation.Generated;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.AnnotationSpec;

public class SkeletonGenerator {
    private final String CLASS_NAME_PREFIX = "Generated";
    private final ClassName superinterface = ClassName.get(
	    "uk.ac.manchester.bauprofiler.json.generator", "JsonConvertableProfile");
    private ProfileContainer container;

    public SkeletonGenerator(ProfileContainer container) {
	this.container = container;
    }

    public String getClassName() {
	return CLASS_NAME_PREFIX + container.className;
    }

    public String getPackageName() {
	return container.packageName;
    }

    public AnnotationSpec[] getAnnotations() {
	List<AnnotationSpec> annotations = new ArrayList<>();
	annotations.add(getGeneratedAnnotation());
	return annotations.toArray(new AnnotationSpec[annotations.size()]);
    }

    private AnnotationSpec getGeneratedAnnotation() {
	return AnnotationSpec.builder(Generated.class)
	    .addMember("value", "$S"
		    , "uk.ac.manchester.bauprofiler.json.generator.CodeGenerator")
	    .addMember("date", "$S", LocalDateTime.now())
	    .build();
    }

    public ClassName getSuperClass() {
	return ClassName.get(container.packageName, container.className);
    }

    public TypeName getSuperInterface() {
	return (container.fullyQualifiedTypeArgName.isPresent())
	    ? ParameterizedTypeName.get(
		    superinterface, getDependencyClass())
	    : superinterface;
    }

    private ClassName getDependencyClass() {
	return getPrefixedDependencyClass("");
    }

    private ClassName getPrefixedDependencyClass(String classNamePrefix) {
	String fullyQualifiedName = container.fullyQualifiedTypeArgName.get();
	String packageName = extractPackageName(fullyQualifiedName);
	String className = extractClassName(fullyQualifiedName);
	return ClassName.get(
		packageName.isEmpty() ? container.packageName : packageName
		, classNamePrefix+className);
    }

    private String extractPackageName(String fullyQualifiedName) {
	int lastDot = fullyQualifiedName.lastIndexOf('.');
	return (lastDot == -1) ? "" : fullyQualifiedName.substring(0, lastDot);
    }

    private String extractClassName(String fullyQualifiedName) {
	int lastDot = fullyQualifiedName.lastIndexOf('.');
	return (lastDot == -1) ? fullyQualifiedName : fullyQualifiedName.substring(lastDot+1);
    }

    public MethodSpec[] getConstructors() {
	List<MethodSpec> constructors = new ArrayList<>();
	for (ExecutableElement constructor : container.constructors)
	    constructors.add(convertConstructor(constructor));
	return constructors.toArray(new MethodSpec[constructors.size()]);
    }

    private MethodSpec convertConstructor(ExecutableElement element) {
	MethodSpec.Builder builder = MethodSpec.constructorBuilder()
	    .addModifiers(Modifier.PUBLIC);
	StringJoiner sj = new StringJoiner(", ", "super(", ")");
	for (VariableElement parameter : element.getParameters()) {
	    builder.addParameter(ParameterSpec.get(parameter));
	    sj.add(parameter.toString());
	}
	return builder.addStatement(sj.toString()).build();
    }

    public MethodSpec[] getMethods() {
	List<MethodSpec> methods = new ArrayList<>();
	if (container.fullyQualifiedTypeArgName.isPresent())
	    methods.add(getDependsMethod());
	return methods.toArray(new MethodSpec[methods.size()]);
    }

    private MethodSpec getDependsMethod() {
	ClassName dependencyClass = getPrefixedDependencyClass(CLASS_NAME_PREFIX);
	return MethodSpec.methodBuilder("depends")
	    .addModifiers(Modifier.PUBLIC)
	    .addStatement("return $T.class", dependencyClass)
	    .returns(ParameterizedTypeName.get(ClassName.get(Class.class), dependencyClass))
	    .build();
    }
}
