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
import java.util.Optional;
import java.time.LocalDateTime;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.Modifier;
import javax.annotation.Generated;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.AnnotationSpec;

import uk.ac.manchester.bauprofiler.core.Profile;
import uk.ac.manchester.bauprofiler.core.converter.ConvertableProfile;

public class SkeletonGenerator {
    private final String CLASS_NAME_PREFIX = "Generated";
    private final ClassName superinterface = ClassName.get(JsonConvertableProfile.class);
    private final ProfileContainer container;

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
	    .addMember("value", "$S", CodeGenerator.class.getName())
	    .addMember("date", "$S", LocalDateTime.now())
	    .build();
    }

    public ClassName getSuperClass() {
	return ClassName.get(container.packageName, container.className);
    }

    public TypeName getSuperInterface() {
	return superinterface;
    }

    public FieldSpec[] getFields() {
	List<FieldSpec> fields = new ArrayList<>();
	fields.add(createIdField());
	fields.add(createDependencyIdField());
	return fields.toArray(new FieldSpec[fields.size()]);
    }

    private FieldSpec createIdField() {
	return FieldSpec.builder(TypeName.INT, "_id")
	    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
	    .initializer("$L", container.classId)
	    .build();
    }

    private FieldSpec createDependencyIdField() {
	return FieldSpec.builder(ParameterizedTypeName.get(
		    ClassName.get(Optional.class), TypeName.INT.box()), "_dependencyId")
	    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
	    .initializer(getDependencyIdInitializer())
	    .build();
    }

    private CodeBlock getDependencyIdInitializer() {
	return CodeBlock.of(getDependencyIdFormat(), getDependencyIdArgs());
    }

    private String getDependencyIdFormat() {
	return container.dependencyId.isPresent() ? "$T.of($L)" : "$T.empty()";
    }

    private Object[] getDependencyIdArgs() {
	List<Object> args = new ArrayList<>();
	args.add(ClassName.get(Optional.class));
	if (container.dependencyId.isPresent())
	    args.add(container.dependencyId.get());
	return args.toArray();
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
	methods.add(getGetIdMethod());
	methods.add(getGetDependencyIdMethod());
	methods.add(getDependsOnMethod());
	return methods.toArray(new MethodSpec[methods.size()]);
    }

    private MethodSpec getGetIdMethod() {
	return MethodSpec.methodBuilder("getId")
	    .addModifiers(Modifier.PUBLIC)
	    .addStatement("return _id")
	    .returns(TypeName.INT)
	    .build();
    }

    private MethodSpec getGetDependencyIdMethod() {
	return MethodSpec.methodBuilder("getDependencyId")
	    .addModifiers(Modifier.PUBLIC)
	    .addStatement("return _dependencyId")
	    .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), TypeName.INT.box()))
	    .build();
    }

    private MethodSpec getDependsOnMethod() {
	return MethodSpec.methodBuilder("dependsOn")
	    .addModifiers(Modifier.PUBLIC)
	    .addParameter(ClassName.get(Profile.class), "dep")
	    .addCode(getDependsOnStatement())
	    .returns(TypeName.BOOLEAN)
	    .build();
    }

    private CodeBlock getDependsOnStatement() {
	return (container.fullyQualifiedDependencyName.isPresent())
	    ? getCustomDependsOnStatement() : getDefaultDependsOnStatement();
    }

    private CodeBlock getCustomDependsOnStatement() {
	ClassName dependency = ClassName.bestGuess(container.fullyQualifiedDependencyName.get());
	return CodeBlock.builder()
	    .beginControlFlow("if (dep instanceof $T)", dependency)
	    .addStatement("return super.predicate(($T) dep)", dependency)
	    .endControlFlow()
	    .addStatement("return false")
	    .build();
    }

    private CodeBlock getDefaultDependsOnStatement() {
	return CodeBlock.builder().addStatement("return false").build();
    }
}
