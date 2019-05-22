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

import static com.google.testing.compile.Compiler.javac;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.Compilation;
import javax.lang.model.element.TypeElement;

public class ProfileExtractorTest {
    private ProfileContainer extractContainerForSourceLines(
	    String fullyQualifiedName, String... lines) {
	ExtractOnly processor = new ExtractOnly();
	Compilation unused = javac()
	    .withProcessors(processor)
	    .compile(JavaFileObjects.forSourceLines(fullyQualifiedName, lines));

	return ProfileExtractor.extract(
		processor.annotatedElements.toArray(new TypeElement[1])[0]);
    }

    @Test (expected=ProfileExtractor.ProfileUnimplementedException.class)
    public void testExtractionFromClassNotImplementingProfile() {
	ProfileContainer container = extractContainerForSourceLines(
		"test.ProfileNotImpl"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.JClass;"
		, "@JClass"
		, "public class ProfileNotImpl {"
		, "}");
    }

    @Test
    public void testExtractionOfProfileTypeArg() {
	ProfileContainer container = extractContainerForSourceLines(
		"test.ProfileImplWithTypeArg"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.JClass;"
		, "import uk.ac.manchester.bauprofiler.core.Profile;"
		, "import uk.ac.manchester.bauprofiler.json.generator.DummyProfileImpl;"
		, "@JClass"
		, "public class ProfileImplWithTypeArg implements Profile<DummyProfileImpl> {"
		, "}");

	assertTrue(container.fullyQualifiedTypeArgName.isPresent());
	assertThat(container.fullyQualifiedTypeArgName.get()
		, equalTo("uk.ac.manchester.bauprofiler.json.generator.DummyProfileImpl"));
    }

    @Test
    public void testExtractionOfPackageName() {
	ProfileContainer container = extractContainerForSourceLines(
		"test.ProfileImpl"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.JClass;"
		, "import uk.ac.manchester.bauprofiler.core.Profile;"
		, "@JClass"
		, "public class ProfileImpl implements Profile {"
		, "}");

	assertThat(container.packageName, equalTo("test"));
    }

    @Test
    public void testExtractionOfClassName() {
	ProfileContainer container = extractContainerForSourceLines(
		"test.ProfileImpl"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.JClass;"
		, "import uk.ac.manchester.bauprofiler.core.Profile;"
		, "@JClass"
		, "public class ProfileImpl implements Profile {"
		, "}");

	assertThat(container.className, equalTo("ProfileImpl"));
    }

    @Test
    public void testExtractionOfConstructors() {
	ProfileContainer container = extractContainerForSourceLines(
		"test.ProfileImpl"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.JClass;"
		, "import uk.ac.manchester.bauprofiler.core.Profile;"
		, "@JClass"
		, "public class ProfileImpl implements Profile {"
		, "  public ProfileImpl() {}"
		, "  public ProfileImpl(String param) {}"
		, "}");

	assertThat(container.constructors.length, equalTo(2));
    }
}
