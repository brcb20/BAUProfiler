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

import static javax.lang.model.util.ElementFilter.constructorsIn;

import java.util.List;
import java.util.Optional;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.DeclaredType;

import uk.ac.manchester.bauprofiler.core.Profile;
import uk.ac.manchester.bauprofiler.core.interfaces.Dependency;

public class ProfileExtractor {
    private static final String PROFILE_FULLY_QUALIFIED_NAME = Profile.class.getName();
    private static final String DEPENDENCY_FULLY_QUALIFIED_NAME = Dependency.class.getName();
    private TypeElement enclosingElement;
    private DeclaredType profile;
    private ProfileContainer container;

    public static ProfileContainer extract(TypeElement enclosingElement) {
	return new ProfileExtractor(enclosingElement).extractIntoContainer();
    }

    private ProfileExtractor(TypeElement enclosingElement) {
	container = new ProfileContainer();
	this.enclosingElement = enclosingElement;
    }

    private ProfileContainer extractIntoContainer() {
	findProfileImplementation();
	guaranteeImplementsProfile();
	populateEnclosingElement();
	populatePackageName();
	populateClassName();
	populateDependencyName();
	populateConstructors();
	return container;
    }

    private void findProfileImplementation() {
	profile = findImplementation(PROFILE_FULLY_QUALIFIED_NAME);
    }

    private DeclaredType findImplementation(String fullyQualifiedName) {
        List<? extends TypeMirror> interfaces = enclosingElement.getInterfaces();
	for (TypeMirror itf : interfaces)
	    if (itf.toString().startsWith(fullyQualifiedName))
		return (DeclaredType) itf;
	return null;
    }

    private void guaranteeImplementsProfile() {
	if (profile == null)
	    throw new ProfileUnimplementedException();
    }

    private void populateEnclosingElement() {
	container.enclosingElement = enclosingElement;
    }

    private void populatePackageName() {
	container.packageName = ((PackageElement) enclosingElement.getEnclosingElement())
            .getQualifiedName().toString();
    }

    private void populateClassName() {
	container.className = enclosingElement.getSimpleName().toString();
    }

    private void populateDependencyName() {
	DeclaredType dependency = findImplementation(DEPENDENCY_FULLY_QUALIFIED_NAME);
	if (dependency == null)
	    setEmptyDependencyName();
	else
	    setDependencyNameFromTypeParam(dependency);
    }

    public void setEmptyDependencyName() {
	container.fullyQualifiedDependencyName = Optional.empty();
    }

    public void setDependencyNameFromTypeParam(DeclaredType dependency) {
	List<? extends TypeMirror> typeArguments = dependency.getTypeArguments();
	if (typeArguments.isEmpty())
	    throw new DependencyRawTypeParameterException();
	container.fullyQualifiedDependencyName = Optional.of(
		getOnlyTypeArgument(typeArguments).toString());
    }

    private TypeMirror getOnlyTypeArgument(List<? extends TypeMirror> typeArguments) {
	return typeArguments.get(0);
    }

    private void populateConstructors() {
        container.constructors = constructorsIn(enclosingElement.getEnclosedElements())
	    .toArray(new ExecutableElement[0]);
    }

    public static class ProfileUnimplementedException extends RuntimeException {}
    public static class DependencyRawTypeParameterException extends RuntimeException {}
}
