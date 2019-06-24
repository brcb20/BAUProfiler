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

import java.util.Optional;
import org.junit.Assert;

import uk.ac.manchester.bauprofiler.json.core.JType;
import uk.ac.manchester.bauprofiler.json.core.BaseNode;
import uk.ac.manchester.bauprofiler.json.core.ObjectNode;
import uk.ac.manchester.bauprofiler.json.annotations.JClass;

public class ContainerSubject {
    private final JsonContainer actual;

    public static ContainerSubject assertThat(JsonContainer actual) {
	return new ContainerSubject(actual);
    }

    private ContainerSubject(JsonContainer actual) {
	this.actual = actual;
    }

    public void hadDefaultJClassInfo() {
	Assert.assertThat(actual.prefixes.length, equalTo(0));
	Assert.assertThat(actual.type, equalTo(JType.OBJECT));
    }

    public void hadMatchingPrefixes(String... prefixes) {
	Assert.assertArrayEquals(prefixes, actual.prefixes);
    }

    public void hadMatchingType(JType type) {
	Assert.assertThat(actual.type, equalTo(type));
    }

    public void hadChildAnnotation() {
	Assert.assertTrue(actual.childNode.isPresent());
    }

    public void hadMatchingChildPrefix(String childPrefix) {
	Assert.assertThat(actual.childNode.get().prefix(), equalTo(childPrefix));
    }

    public void hadDefaultChildType() {
	Assert.assertThat(actual.childNode.get(), instanceOf(ObjectNode.class));
    }

    public void hadInvisibleFields(int count) {
	Assert.assertThat(actual.invisibleFields, equalTo(count));
    }

    public FieldSubject hadJFieldNamed(String fieldName) {
	FieldSubject subject = getSubjectForFieldNamed(fieldName, actual.fields);
	Assert.assertNotNull("No field named "+fieldName, subject);
	return subject;
    }

    private FieldSubject getSubjectForFieldNamed(String fieldName, Field[] fields) {
	for (Field f : fields)
	    if (fieldName.equals(f.key))
		return new FieldSubject(f);
	return null;
    }

    public FieldSubject hadChildFieldNamed(String fieldName) {
	FieldSubject subject = getSubjectForFieldNamed(fieldName, actual.childFields);
	Assert.assertNotNull("No child field named "+fieldName, subject);
	return subject;
    }

    // TODO: Extend com.google.common.truth.Subject
    public class FieldSubject {
	private Field actual;

	private FieldSubject(Field actual) {
	    this.actual = actual;
	}

	public void forOriginalFieldNamed(String originalFieldName) {
	    Assert.assertThat(actual.valueFormatArg, equalTo(originalFieldName));
	}

	public void withMatchingValueFormat(String valueFormat) {
	    Assert.assertThat(actual.valueFormat, equalTo(valueFormat));
	}

	public void withMatchingValueFormatArg(String valueFormatArg) {
	    Assert.assertThat(actual.valueFormatArg, equalTo(valueFormatArg));
	}

	public void markedInvisible() {
	    Assert.assertTrue(actual.isInvisible);
	}
    }
}
