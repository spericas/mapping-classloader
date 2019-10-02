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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Class MappingClassLoaderTest.
 */
public class MappingClassLoaderTest {

    private static final String JAVAX_APP_CLASS = "javax.ws.rs.core.Application";
    private static final String JAKARTA_APP_CLASS = "jakarta.ws.rs.core.Application";

    private static ClassLoader oldClassLoader;

    @BeforeClass
    public static void setClassLoader() {
        MappingClassLoader loader = new MappingClassLoader("jakarta.ws", "javax.ws");
        oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
    }

    @AfterClass
    public static void restoreClassLoader() {
        Thread.currentThread().setContextClassLoader(oldClassLoader);
    }

    @Test
    public void testSimpleMap() throws Exception {
        System.out.println("Attempting to load '" + JAKARTA_APP_CLASS + "' ...");
        Class<?> applicationClass = Thread.currentThread().getContextClassLoader().loadClass(JAKARTA_APP_CLASS);
        System.out.println("Actual class loaded '" + applicationClass.getName() + "'.");
        assertThat(applicationClass.getName(), is(JAVAX_APP_CLASS));
    }
}
