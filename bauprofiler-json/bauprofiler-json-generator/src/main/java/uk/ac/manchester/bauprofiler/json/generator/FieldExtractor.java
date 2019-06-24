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

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.ArrayType;

import uk.ac.manchester.bauprofiler.json.core.JType;
import uk.ac.manchester.bauprofiler.json.annotations.*;

public class FieldExtractor {
    VariableElement element;
    String simpleName;
    JField jfield;

    private FieldExtractor(VariableElement element) {
	this.element = element;
	simpleName = element.getSimpleName().toString();
	jfield = element.getAnnotation(JField.class);
    }

    public static Field extract(VariableElement element) {
	return new FieldExtractor(element).extractIntoField();
    }

    private Field extractIntoField() {
	TypeMirror type = element.asType();
	Field field = new Field();
	field.key = getKey();
	field.valueFormat = getValueFormat(type);
	field.valueFormatArg = getValueFormatArg(type);
	field.isInvisible = isInvisible();
	return field;
    }

    private String getKey() {
	return jfield.prefix()
	+ ((jfield.key().isEmpty()) ? simpleName : jfield.key())
	+ jfield.postfix();
    }

    private String getValueFormat(TypeMirror type) {
	String valueFormat;
	if (hasOverloadedFormat())
	    valueFormat = getOverloadedFormat();
	else if (isStringType(type))
	    valueFormat = "\"%s\"";
	else if (isStringArrayType(type))
	    valueFormat = "%s";
	else if (isPrimitiveType(type))
	    valueFormat = getPrimitiveFormat(type);
	else if (isPrimitiveArrayType(type))
	    valueFormat = "%s";
	else
	    throw new InvalidTypeException();
	return valueFormat;
    }

    private boolean hasOverloadedFormat() {
	return element.getAnnotation(NumberFormat.class) != null;
    }

    private String getOverloadedFormat() {
	return element.getAnnotation(NumberFormat.class).value();
    }

    private boolean isStringType(TypeMirror type) {
	return type.getKind().equals(TypeKind.DECLARED)
	    && type.toString().equals("java.lang.String");
    }

    private boolean isStringArrayType(TypeMirror type) {
	return isArrayType(type) && isStringComponentType(type);
    }

    private boolean isArrayType(TypeMirror type) {
	return type.getKind().equals(TypeKind.ARRAY);
    }

    private boolean isStringComponentType(TypeMirror type) {
	return isStringType(((ArrayType) type).getComponentType());
    }

    private boolean isPrimitiveType(TypeMirror type) {
	return type.getKind().isPrimitive();
    }

    private String getPrimitiveFormat(TypeMirror type) {
	TypeKind kind = type.getKind();
	if (isNumeric(kind))
	    return "%d";
	else if (isDecimal(kind))
	    return "%f";
	throw new InvalidPrimitiveTypeException();
    }

    private boolean isNumeric(TypeKind kind) {
	return (kind.equals(TypeKind.INT) || kind.equals(TypeKind.LONG));
    }

    private boolean isDecimal(TypeKind kind) {
	return (kind.equals(TypeKind.DOUBLE) || kind.equals(TypeKind.FLOAT));
    }

    private boolean isPrimitiveArrayType(TypeMirror type) {
	return isArrayType(type) && isPrimitiveComponentType(type);
    }

    private boolean isPrimitiveComponentType(TypeMirror type) {
	return isPrimitiveType(((ArrayType) type).getComponentType());
    }

    private String getValueFormatArg(TypeMirror type) {
	return (isArrayType(type)) ? wrapArrayArg(type) : simpleName;
    }

    private String wrapArrayArg(TypeMirror type) {
	return (isPrimitiveComponentType(type))
		? wrapPrimitiveArrayArg(simpleName) : wrapStringArrayArg(simpleName);
    }

    private String wrapPrimitiveArrayArg(String arg) {
	return "java.util.Arrays.toString("+arg+")";
    }

    private String wrapStringArrayArg(String arg) {
	return "JsonConvertableProfile.arrayToString("+arg+")";
    }

    private boolean isInvisible() {
	return element.getAnnotation(Invisible.class) != null;
    }

    public static class InvalidTypeException extends RuntimeException {}
    public static class InvalidPrimitiveTypeException extends RuntimeException {}
}
