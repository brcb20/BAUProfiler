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

import java.util.Set;
import java.util.Collections;

import com.google.common.collect.ImmutableSet;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

import uk.ac.manchester.bauprofiler.json.annotations.JClass;

public class ExtractOnly extends AbstractProcessor {
    Set<TypeElement> annotatedElements = ImmutableSet.of();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
	super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotatedElements = ImmutableSet.<TypeElement>builder()
	    .addAll(annotatedElements)
	    .addAll((Set<TypeElement>)roundEnv.getElementsAnnotatedWith(JClass.class))
	    .build();
	return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
	return ImmutableSet.of(JClass.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
	return SourceVersion.RELEASE_8;
    }
}
