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
import static uk.ac.manchester.bauprofiler.json.generator.ContainerSubject.assertThat;

import java.util.Set;

import org.junit.Test;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.Compilation;
import javax.lang.model.element.TypeElement;

import uk.ac.manchester.bauprofiler.json.core.JType;

public class AnnotationExtractorTest {
    private JsonContainer extractContainerForSourceLines(
	    String fullyQualifiedName, String... lines) {
	ExtractOnly processor = new ExtractOnly();
	Compilation unused = javac()
	    .withProcessors(processor)
	    .compile(JavaFileObjects.forSourceLines(fullyQualifiedName, lines));

	return AnnotationExtractor.extract(getOnlyElement(processor.annotatedElements));
    }

    private TypeElement getOnlyElement(Set<TypeElement> elements) {
	return elements.toArray(new TypeElement[1])[0];
    }

    @Test
    public void testExtractionOfDefaultJClassAnnotation() {
	JsonContainer container = extractContainerForSourceLines(
		"test.DefaultJClassAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class DefaultJClassAnnotation {"
		    , "  @JField protected String requiresAtLeastOneField;"
		, "}");

	assertThat(container).hadDefaultJClassInfo();
    }

    @Test
    public void testExtractionOfCustomJClassAnnotationPath() {
	JsonContainer container = extractContainerForSourceLines(
		"test.CustomJClassAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass("
		, "  path=\"my/test/path\""
		, ")"
		, "public class CustomJClassAnnotation {"
		, "  @JField protected String requiresAtLeastOneField;"
		, "}");

	assertThat(container).hadMatchingPrefixes("my", "test", "path");
    }

    @Test
    public void testExtractionOfCustomJClassAnnotationType() {
	JsonContainer container = extractContainerForSourceLines(
		"test.CustomJClassAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.core.JType;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass("
		, "  type=JType.OBJECT_ARRAY"
		, ")"
		, "public class CustomJClassAnnotation {"
		, "  @JField protected String requiresAtLeastOneField;"
		, "}");

	assertThat(container).hadMatchingType(JType.OBJECT_ARRAY);
    }

    @Test
    public void testExtractionOfChildAnnotation() {
	JsonContainer container = extractContainerForSourceLines(
		"test.ChildAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass(path=\"my/test/path\")"
		, "@Child(key=\"child\")"
		, "public class ChildAnnotation {"
		, "  @JField @ChildField protected String requiresAtLeastOneChildField;"
		, "}");

	assertThat(container).hadChildAnnotation();
    }

    @Test
    public void testExtractionOfChildAnnotationKey() {
	JsonContainer container = extractContainerForSourceLines(
		"test.ChildAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass(path=\"my/test/path\")"
		, "@Child(key=\"child\")"
		, "public class ChildAnnotation {"
		, "  @JField @ChildField protected String requiresAtLeastOneChildField;"
		, "}");

	assertThat(container).hadMatchingChildPrefix("child");
    }

    @Test
    public void testExtractionOfDefaultChildAnnotationType() {
	JsonContainer container = extractContainerForSourceLines(
		"test.ChildAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass(path=\"my/test/path\")"
		, "@Child(key=\"child\")"
		, "public class ChildAnnotation {"
		, "  @JField @ChildField protected String requiresAtLeastOneChildField;"
		, "}");

	assertThat(container).hadDefaultChildType();
    }

    @Test (expected=AnnotationExtractor.MissingChildFieldsException.class)
    public void testExtractionOfChildAnnotationWithNoChildFields() {
	JsonContainer container = extractContainerForSourceLines(
		"test.ChildAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass(path=\"my/test/path\")"
		, "@Child(key=\"child\")"
		, "public class ChildAnnotation {"
		, "  @JField protected String requiresAtLeastOneChildField;"
		, "}");
    }

    @Test
    public void testExtractionOfJField() {
	JsonContainer container = extractContainerForSourceLines(
		"test.JFieldAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class JFieldAnnotation {"
		, "  @JField protected int quantity;"
		, "}");

	assertThat(container).hadJFieldNamed("quantity");
    }

    @Test
    public void testExtractionOfJFieldWithSubstitutedName() {
	JsonContainer container = extractContainerForSourceLines(
		"test.JFieldAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class JFieldAnnotation {"
		, "  @JField(key=\"qty\") protected int quantity;"
		, "}");

	assertThat(container).hadJFieldNamed("qty").forOriginalFieldNamed("quantity");
    }

    @Test
    public void testExtractionOfJFieldWithPrefix() {
	JsonContainer container = extractContainerForSourceLines(
		"test.JFieldAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class JFieldAnnotation {"
		, "  @JField(prefix=\"_\") protected int quantity;"
		, "}");

	assertThat(container).hadJFieldNamed("_quantity").forOriginalFieldNamed("quantity");
    }

    @Test
    public void testExtractionOfJFieldWithPostfix() {
	JsonContainer container = extractContainerForSourceLines(
		"test.JFieldAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class JFieldAnnotation {"
		, "  @JField(postfix=\"_\") protected int quantity;"
		, "}");

	assertThat(container).hadJFieldNamed("quantity_").forOriginalFieldNamed("quantity");
    }

    @Test (expected=FieldExtractor.InvalidTypeException.class)
    public void testExtractionOfJFieldValueFormatForObject() {
	JsonContainer container = extractContainerForSourceLines(
		"test.JFieldAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class JFieldAnnotation {"
		, "  @JField protected Object myObject;"
		, "}");
    }

    @Test
    public void testExtractionOfJFieldValueFormatForString() {
	JsonContainer container = extractContainerForSourceLines(
		"test.JFieldAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class JFieldAnnotation {"
		, "  @JField protected String myString;"
		, "}");

	assertThat(container).hadJFieldNamed("myString").withMatchingValueFormat("\"%s\"");
    }

    @Test
    public void testExtractionOfJFieldValueFormatForInt() {
	JsonContainer container = extractContainerForSourceLines(
		"test.JFieldAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class JFieldAnnotation {"
		, "  @JField protected int myInt;"
		, "}");

	assertThat(container).hadJFieldNamed("myInt").withMatchingValueFormat("%d");
    }

    @Test
    public void testExtractionOfJFieldValueFormatForLong() {
	JsonContainer container = extractContainerForSourceLines(
		"test.JFieldAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class JFieldAnnotation {"
		, "  @JField protected long myLong;"
		, "}");

	assertThat(container).hadJFieldNamed("myLong").withMatchingValueFormat("%d");
    }

    @Test
    public void testExtractionOfJFieldValueFormatForFloat() {
	JsonContainer container = extractContainerForSourceLines(
		"test.JFieldAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class JFieldAnnotation {"
		, "  @JField protected float myFloat;"
		, "}");

	assertThat(container).hadJFieldNamed("myFloat").withMatchingValueFormat("%f");
    }

    @Test
    public void testExtractionOfJFieldValueFormatOverloadedByNumberFormat() {
	JsonContainer container = extractContainerForSourceLines(
		"test.NumberFormatAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class NumberFormatAnnotation {"
		, "  @JField @NumberFormat(\"%.2f\") protected float myFloat;"
		, "}");

	assertThat(container).hadJFieldNamed("myFloat").withMatchingValueFormat("%.2f");
    }

    @Test (expected=FieldExtractor.InvalidPrimitiveTypeException.class)
    public void testExtractionOfJFieldValueFormatForOtherPrimitive() {
	JsonContainer container = extractContainerForSourceLines(
		"test.JFieldAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class JFieldAnnotation {"
		, "  @JField protected byte myByte;"
		, "}");
    }

    @Test
    public void testExtractionOfJFieldValueFormatArgForIntArray() {
	JsonContainer container = extractContainerForSourceLines(
		"test.JFieldAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class JFieldAnnotation {"
		, "  @JField protected int[] myIntArray;"
		, "}");

	assertThat(container)
	    .hadJFieldNamed("myIntArray")
	    .withMatchingValueFormatArg("java.util.Arrays.toString(myIntArray)");
    }

    @Test
    public void testExtractionOfJFieldValueFormatArgForStringArray() {
	JsonContainer container = extractContainerForSourceLines(
		"test.JFieldAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class JFieldAnnotation {"
		, "  @JField protected String[] myStringArray;"
		, "}");

	assertThat(container)
	    .hadJFieldNamed("myStringArray")
	    .withMatchingValueFormatArg("JsonConvertableProfile.arrayToString(myStringArray)");
    }

    @Test
    public void testExtractionOfIndividualJFieldMarkedInvisible() {
	JsonContainer container = extractContainerForSourceLines(
		"test.InvisibleAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class InvisibleAnnotation {"
		, "  @JField protected String cannotAllBeInvisible;"
		, "  @JField @Invisible protected String myInvisibleString;"
		, "}");

	assertThat(container)
	    .hadJFieldNamed("myInvisibleString")
	    .markedInvisible();
    }

    @Test
    public void testExtractionOfAllJFieldMarkedInvisible() {
	JsonContainer container = extractContainerForSourceLines(
		"test.InvisibleAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class InvisibleAnnotation {"
		, "  @JField @Invisible protected String myInvisibleString;"
		, "  @JField protected String myString;"
		, "  @JField @Invisible protected int myInvisibleInt;"
		, "  @JField protected int myInt;"
		, "}");

	assertThat(container).hadInvisibleFields(2);
    }

    @Test (expected=AnnotationExtractor.MissingChildTypeAnnotationException.class)
    public void testExtractionOfChildJFieldWithoutTypeLevelChildAnnotation() {
	JsonContainer container = extractContainerForSourceLines(
		"test.ChildFieldAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class ChildFieldAnnotation {"
		, "  @JField @ChildField protected int quantity;"
		, "}");
    }

    @Test
    public void testExtractionOfChildJField() {
	JsonContainer container = extractContainerForSourceLines(
		"test.ChildFieldAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "@Child(key=\"child\")"
		, "public class ChildFieldAnnotation {"
		, "  @JField @ChildField protected int quantity;"
		, "}");

	assertThat(container).hadChildFieldNamed("quantity");
    }

    @Test (expected=AnnotationExtractor.NoVisibleChildFieldsException.class)
    public void testExtractionOfSingleChildFieldMarkedInvisible() {
	JsonContainer container = extractContainerForSourceLines(
		"test.InvisibleChildAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "@Child(key=\"child\")"
		, "public class InvisibleChildAnnotation {"
		, "  @JField protected String cannotAllBeInvisible;"
		, "  @JField @ChildField @Invisible protected String myInvisibleChildString;"
		, "}");
    }

    @Test
    public void testExtractionOfChildFieldMarkedInvisible() {
	JsonContainer container = extractContainerForSourceLines(
		"test.InvisibleChildAnnotation"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "@Child(key=\"child\")"
		, "public class InvisibleChildAnnotation {"
		, "  @JField @ChildField protected String cannotAllBeInvisible;"
		, "  @JField @ChildField @Invisible protected String myInvisibleChildString;"
		, "}");

	assertThat(container)
	    .hadChildFieldNamed("myInvisibleChildString")
	    .markedInvisible();
    }

    @Test (expected=AnnotationExtractor.ProtectedModifierException.class)
    public void testExtractionOfJFieldWithModifierOtherThanProtected() {
	JsonContainer container = extractContainerForSourceLines(
		"test.PrivateJField"
		, "package test;"
		, "import uk.ac.manchester.bauprofiler.json.annotations.*;"
		, "@JClass"
		, "public class PrivateJField {"
		, "  @JField private int myPrivateField;"
		, "}");
    }
}
