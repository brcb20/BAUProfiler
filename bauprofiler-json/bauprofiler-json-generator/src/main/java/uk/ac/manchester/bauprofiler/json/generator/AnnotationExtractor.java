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

import static javax.lang.model.util.ElementFilter.fieldsIn;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import uk.ac.manchester.bauprofiler.json.core.JType;
import uk.ac.manchester.bauprofiler.json.core.BaseNode;
import uk.ac.manchester.bauprofiler.json.annotations.*;

public class AnnotationExtractor {
    private JsonContainer container;
    private TypeElement enclosingElement;
    private JClass jclass;
    private Child child;
    private List<VariableElement> fields = new ArrayList<>();
    private List<VariableElement> childFields = new ArrayList<>();

    private AnnotationExtractor(TypeElement enclosingElement) {
	container = new JsonContainer();
	this.enclosingElement = enclosingElement;
	jclass = enclosingElement.getAnnotation(JClass.class);
	child = enclosingElement.getAnnotation(Child.class);
    }

    public static JsonContainer extract(TypeElement element) {
	return new AnnotationExtractor(element).extractIntoContainer();
    }

    /* TODO: separate validation from extraction */
    private JsonContainer extractIntoContainer() {
	populateJClassInfo();
	populateChildInfo();
	filterFields();
	guaranteeHasJFields();
	guaranteeFilteredFieldsHaveProtectedModifier();
	guaranteeChildInfoMatchesChildFields();
	populateFieldInfo();
	populateChildFieldInfo();
	populateInvisibleInfo();
	checkHasVisibleFields();
	checkHasVisibleChildFields();
	return container;
    }

    private void populateJClassInfo() {
	container.prefixes = getPrefixes();
	container.type = getType();
    }

    private String[] getPrefixes() {
	String path = jclass.path();
        int start = 0, end = path.length();
	if (path.startsWith("/")) { ++start; }
        if (path.endsWith("/")) { --end; }
        return (start == end) ? new String[0] : path.substring(start, end).split("\\/+");
    }

    private JType getType() {
	return jclass.type();
    }

    private void populateChildInfo() {
	container.childNode = convertChildInfoToNode();
    }

    private Optional<BaseNode> convertChildInfoToNode() {
	return (hasChildAnnotation())
	    ? Optional.of(instantiateChildNode())
	    : Optional.empty();
    }

    private boolean hasChildAnnotation() {
	return child != null;
    }

    private BaseNode instantiateChildNode() {
	try {
	    return child.type()
		.getNodeClass()
		.getConstructor(String.class, Long.TYPE)
		.newInstance(child.key(), 0);
	} catch (Exception e) {
	    throw new NodeConstructorException();
	}
    }

    private void filterFields() {
        for (VariableElement field : fieldsIn(enclosingElement.getEnclosedElements()))
	    if (isAnnotatedWithJField(field))
		separateChildField(field);
    }

    private boolean isAnnotatedWithJField(VariableElement field) {
	return field.getAnnotation(JField.class) != null;
    }

    private void separateChildField(VariableElement field) {
	if (isChildField(field))
	    childFields.add(field);
	else
	    fields.add(field);
    }

    private boolean isChildField(VariableElement field) {
	return field.getAnnotation(ChildField.class) != null;
    }

    private void guaranteeHasJFields() {
	if (fields.size() == 0 && childFields.size() == 0)
	    throw new MissingJFieldAnnotationsException();
    }

    private void guaranteeFilteredFieldsHaveProtectedModifier() {
	checkFieldsHaveProtectedModifier(fields);
	checkFieldsHaveProtectedModifier(childFields);
    }

    private void checkFieldsHaveProtectedModifier(List<VariableElement> elements) {
	for (VariableElement elem : elements)
	    if (!elem.getModifiers().contains(Modifier.PROTECTED))
		throw new ProtectedModifierException();
    }

    private void guaranteeChildInfoMatchesChildFields() {
	if (container.childNode.isPresent())
	    checkHasChildFields();
	else
	    checkHasNoChildFields();
    }

    private void checkHasChildFields() {
	if (childFields.isEmpty())
	    throw new MissingChildFieldsException();
    }

    private void checkHasNoChildFields() {
	if (!childFields.isEmpty())
	    throw new MissingChildTypeAnnotationException();
    }

    private void populateFieldInfo() {
	container.fields = extractFieldInfo(fields);
    }

    private Field[] extractFieldInfo(List<VariableElement> elements) {
	List<Field> fieldInfo = new ArrayList<>();
	for (VariableElement elem : elements)
	    fieldInfo.add(FieldExtractor.extract(elem));
	return fieldInfo.toArray(new Field[fieldInfo.size()]);
    }

    private void populateChildFieldInfo() {
	container.childFields = extractFieldInfo(childFields);
    }

    private void populateInvisibleInfo() {
	container.invisibleFields = countInvisibleFields(container.fields);
	container.invisibleChildFields = countInvisibleFields(container.childFields);
    }

    private int countInvisibleFields(Field[] fields) {
	int count = 0;
	for (Field f : fields)
	    if (f.isInvisible)
		++count;
	return count;
    }

    private void checkHasVisibleFields() {
	if (hasNoVisibleFields())
	    throw new NoVisibleFieldsException();
    }

    private boolean hasNoVisibleFields() {
	return container.invisibleFields == container.fields.length
	    && hasNoVisibleChildFields();
    }

    private boolean hasNoVisibleChildFields() {
	return container.invisibleChildFields == container.childFields.length;
    }

    private void checkHasVisibleChildFields() {
	if (container.childFields.length > 0 && hasNoVisibleChildFields())
	    throw new NoVisibleChildFieldsException();
    }

    public static class MissingChildTypeAnnotationException extends RuntimeException {}
    public static class MissingChildFieldsException extends RuntimeException {}
    public static class ProtectedModifierException extends RuntimeException {}
    public static class MissingJFieldAnnotationsException extends RuntimeException {}
    public static class NoVisibleFieldsException extends RuntimeException {}
    public static class NoVisibleChildFieldsException extends RuntimeException {}
    public static class NodeConstructorException extends RuntimeException {
	public NodeConstructorException() {
	    super("Subclasses of BaseNode require constructor init args of type String, long");
	}
    }
}
