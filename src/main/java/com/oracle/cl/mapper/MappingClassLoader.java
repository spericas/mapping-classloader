/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.cl.mapper;

import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

;

/**
 * MappingClassLoader class.
 */
public class MappingClassLoader extends ClassLoader {

    static final String DEFAULT_SOURCE_PACKAGE = "jakarta";
    static final String DEFAULT_TARGET_PACKAGE = "javax";

    private String sourcePackage;
    private String targetPackage;
    private String sourcePackagePath;
    private String targetPackagePath;

    public MappingClassLoader() {
        this(ClassLoader.getSystemClassLoader());
    }

    public MappingClassLoader(ClassLoader parent) {
        super(parent);
        initialize(DEFAULT_SOURCE_PACKAGE, DEFAULT_TARGET_PACKAGE);
    }

    public MappingClassLoader(String sourcePackage, String targetPackage) {
        initialize(sourcePackage, targetPackage);
    }

    public MappingClassLoader(String sourcePackage, String targetPackage, ClassLoader parent) {
        super(parent);
        initialize(sourcePackage, targetPackage);
    }

    private void initialize(String sourcePackage, String targetPackage) {
        this.sourcePackage = !sourcePackage.endsWith(".") ? sourcePackage :
                sourcePackage.substring(0, sourcePackage.length() - 1);
        this.targetPackage = !targetPackage.endsWith(".") ? targetPackage :
                targetPackage.substring(0, targetPackage.length() - 1);
        this.sourcePackagePath = this.sourcePackage.replace('.', '/');
        this.targetPackagePath = this.targetPackage.replace('.', '/');
    }

    @Override
    public Class<?> loadClass(String typeName) throws ClassNotFoundException {
        String baseName = findBaseName(typeName, '.');
        String packageName = findClassPackage(typeName, '.');

        // If not mappable, use regular loading method
        if (!packageName.startsWith(sourcePackage)) {
            return super.loadClass(typeName);
        }

        StringBuilder newNameBuilder = new StringBuilder();
        newNameBuilder.append(targetPackage)
                .append('.')
                .append(packageName.substring(sourcePackage.length() + 1))
                .append('.')
                .append(baseName);
        String newTypeName = newNameBuilder.toString();

        byte[] mappedBytecode;
        try {
            mappedBytecode = mapPackageNames(newTypeName);
        } catch (IOException e) {
            throw new RuntimeException("Unable to map class " + typeName);
        }

        return defineClass(newTypeName, mappedBytecode, 0, mappedBytecode.length);
    }

    private byte[] mapPackageNames(String typeName) throws IOException {
        ClassReader classReader = new ClassReader(typeName);
        ClassWriter classWriter = new ClassWriter(classReader, 0);
        Remapper mapper = new PackageNameMapper();
        classReader.accept(new ClassRemapper(classWriter, mapper), 0);
        return classWriter.toByteArray();
    }

    private class PackageNameMapper extends Remapper {

        @Override
        public String map(String typeName) {
            String packageName = findClassPackage(typeName, '/');
            if (packageName.startsWith(sourcePackagePath)) {
                String baseName = findBaseName(typeName, '/');
                StringBuilder newTypeName = new StringBuilder();
                newTypeName.append(targetPackagePath)
                        .append('/')
                        .append(packageName.substring(sourcePackagePath.length() + 1))
                        .append('/')
                        .append(baseName);
                return newTypeName.toString();
            }
            return typeName;
        }
    }

    private static String findClassPackage(String className, char delim) {
        final int k = className.lastIndexOf(delim);
        return k > 0 ? className.substring(0, k) : className;
    }

    private static String findBaseName(String className, char delim) {
        final int k = className.lastIndexOf(delim);
        return k > 0 ? className.substring(k + 1) : className;
    }
}