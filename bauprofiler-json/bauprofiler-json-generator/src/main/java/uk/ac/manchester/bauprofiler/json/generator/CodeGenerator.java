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

import java.io.IOException;

import javax.lang.model.element.Modifier;

import javax.annotation.processing.Filer;

import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.JavaFile;

public class CodeGenerator {
    private final SkeletonGenerator skeleton;
    private final BodyGenerator body;
    private JavaFile javaFile;

    private CodeGenerator(SkeletonGenerator skeleton, BodyGenerator body) {
	this.skeleton = skeleton;
	this.body = body;
    }

    public static CodeGenerator build(SkeletonGenerator skeleton, BodyGenerator body) {
	CodeGenerator cg = new CodeGenerator(skeleton, body);
	cg.build();
	return cg;
    }

    private void build() {
	TypeSpec.Builder builder = TypeSpec.classBuilder(skeleton.getClassName())
	    .addModifiers(Modifier.PUBLIC)
	    .superclass(skeleton.getSuperClass())
	    .addSuperinterface(skeleton.getSuperInterface());
	for (AnnotationSpec as : skeleton.getAnnotations())
	    builder.addAnnotation(as);
	for (FieldSpec fs : skeleton.getFields())
	    builder.addField(fs);
	for (FieldSpec fs : body.getFields())
	    builder.addField(fs);
	for (MethodSpec ms : skeleton.getConstructors())
	    builder.addMethod(ms);
	for (MethodSpec ms : skeleton.getMethods())
	    builder.addMethod(ms);
	for (MethodSpec ms : body.getMethods())
	    builder.addMethod(ms);
	javaFile = JavaFile.builder(skeleton.getPackageName(), builder.build())
	    .skipJavaLangImports(true)
	    .build();
    }

    public void writeTo(Appendable out) throws IOException {
	javaFile.writeTo(out);
    }

    public void writeTo(Filer filer) throws IOException {
	javaFile.writeTo(filer);
    }
}
