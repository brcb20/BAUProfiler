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
import java.io.IOException;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.Filer;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

import com.google.common.collect.ImmutableSet;

import uk.ac.manchester.bauprofiler.json.annotations.JClass;

public class AnnotationProcessor extends AbstractProcessor {
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
	super.init(processingEnv);
	this.filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<TypeElement> annotatedElements = (Set<TypeElement>)roundEnv
            .getElementsAnnotatedWith(JClass.class);
	PersistentEncoder encoder = loadEncoder();
	for (TypeElement elem : annotatedElements)
	    writeToFiler(generateCode(elem, encoder));
	persistEncoder(encoder);
	return true;
    }

    private PersistentEncoder loadEncoder() {
	try {
	    return PersistentEncoder.load();
	} catch (ClassNotFoundException | IOException e) {
	    throw new RuntimeException(e);
	}
    }

    private CodeGenerator generateCode(TypeElement element, Encoder encoder) {
	return CodeGenerator.build(
		new SkeletonGenerator(
		    ProfileExtractor.extract(element))
		, new BodyGenerator(
		    AnnotationExtractor.extract(element)
		    , encoder)
		);
    }

    private void writeToFiler(CodeGenerator codeGenerator) {
	try {
	    codeGenerator.writeTo(filer);
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
    }

    private void persistEncoder(PersistentEncoder encoder) {
	try {
	    encoder.persist();
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
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
