/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared.plugins;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChainingClassLoaderTest {
    @Test(expected = ClassNotFoundException.class)
    public void loadThrowsClassNotFoundExceptionIfClassDoesNotExist() throws Exception {
        final ChainingClassLoader chainingClassLoader = new ChainingClassLoader(getClass().getClassLoader());
        chainingClassLoader.loadClass("ThisClassHopeFullyDoesNotExist" + Instant.now().toEpochMilli());
    }

    @Test
    public void loadReturnsClassFromParentClassLoader() throws Exception {
        final ClassLoader parent = getClass().getClassLoader();
        final ChainingClassLoader chainingClassLoader = new ChainingClassLoader(parent);
        final String className = "org.graylog2.shared.plugins.ChainingClassLoaderTest$Dummy";
        final Class<?> aClass = chainingClassLoader.loadClass(className);

        assertThat(aClass).isNotNull();
        assertThat(aClass.getSimpleName()).isEqualTo("Dummy");
        assertThat(aClass.getClassLoader()).isSameAs(parent);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void loadReturnsClassFromChildClassLoader() throws Exception {
        final String className = "com.example.this.class.does.not.exist.Cls";
        final ClassLoader parent = getClass().getClassLoader();
        final ClassLoader child = mock(ClassLoader.class);
        final Class<Dummy> dummyClass = Dummy.class;
        when(child.loadClass(className)).thenReturn((Class) dummyClass);

        final ChainingClassLoader chainingClassLoader = new ChainingClassLoader(parent);
        chainingClassLoader.addClassLoader(child);

        assertThat(chainingClassLoader.getClassLoaders())
                .hasSize(2)
                .containsExactly(parent, child);

        final Class<?> aClass = chainingClassLoader.loadClass(className);
        assertThat(aClass).isNotNull();
        assertThat(aClass).isSameAs(dummyClass);
    }

    @Test
    public void getResourceAsStreamReturnsNullIfResourceDoesNotExist() throws Exception {
        final ChainingClassLoader chainingClassLoader = new ChainingClassLoader(getClass().getClassLoader());
        final InputStream stream = chainingClassLoader.getResourceAsStream("ThisClassHopeFullyDoesNotExist" + Instant.now().toEpochMilli());
        assertThat(stream).isNull();
    }

    @Test
    public void getResourceAsStreamReturnsStreamFromChildClassLoader() throws Exception {
        final ClassLoader parent = getClass().getClassLoader();
        final ClassLoader child = mock(ClassLoader.class);
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("foobar".getBytes(StandardCharsets.UTF_8));
        when(child.getResourceAsStream("name")).thenReturn(inputStream);

        final ChainingClassLoader chainingClassLoader = new ChainingClassLoader(parent);
        chainingClassLoader.addClassLoader(child);

        final InputStream stream = chainingClassLoader.getResourceAsStream("name");
        final ByteArrayInputStream expected = new ByteArrayInputStream("foobar".getBytes(StandardCharsets.UTF_8));
        assertThat(stream).hasSameContentAs(expected);
    }

    @Test
    public void getResourceReturnsNullIfResourceDoesNotExist() throws Exception {
        final ChainingClassLoader chainingClassLoader = new ChainingClassLoader(getClass().getClassLoader());
        final URL resource = chainingClassLoader.getResource("ThisClassHopeFullyDoesNotExist" + Instant.now().toEpochMilli());
        assertThat(resource).isNull();
    }

    @Test
    public void getResourceReturnsURLFromChildClassLoader() throws Exception {
        final ClassLoader parent = getClass().getClassLoader();
        final ClassLoader child = mock(ClassLoader.class);
        final URL url = new URL("file://test");
        when(child.getResource("name")).thenReturn(url);

        final ChainingClassLoader chainingClassLoader = new ChainingClassLoader(parent);
        chainingClassLoader.addClassLoader(child);

        final URL resource = chainingClassLoader.getResource("name");
        assertThat(resource).isEqualTo(url);
    }

    @Test
    public void getResourcesReturnsEmptyEnumerationIfResourceDoesNotExist() throws Exception {
        final ChainingClassLoader chainingClassLoader = new ChainingClassLoader(getClass().getClassLoader());
        final Enumeration<URL> resources = chainingClassLoader.getResources("ThisClassHopeFullyDoesNotExist" + Instant.now().toEpochMilli());
        assertThat(resources.hasMoreElements()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getResourcesReturnsEnumerationFromChildClassLoader() throws Exception {
        final ClassLoader parent = getClass().getClassLoader();
        final ClassLoader child = mock(ClassLoader.class);
        final Enumeration<URL> urls = Collections.enumeration(Collections.singleton(new URL("file://test")));
        when(child.getResources("name")).thenReturn(urls);

        final ChainingClassLoader chainingClassLoader = new ChainingClassLoader(parent);
        chainingClassLoader.addClassLoader(child);

        final Enumeration<URL> resources = chainingClassLoader.getResources("name");
        assertThat(Collections.list(resources)).containsExactly(new URL("file://test"));
    }

    @Test
    public void getClassLoadersReturnsListOfClassLoaders() throws Exception {
        final ClassLoader parent = getClass().getClassLoader();
        final ClassLoader child = new URLClassLoader(new URL[0], parent);
        final ChainingClassLoader chainingClassLoader = new ChainingClassLoader(parent);
        chainingClassLoader.addClassLoader(child);

        assertThat(chainingClassLoader.getClassLoaders())
                .hasSize(2)
                .containsExactly(parent, child);
    }

    @Test
    public void addClassLoaderAddsClassLoaderToList() throws Exception {
        final ClassLoader parent = getClass().getClassLoader();
        final ClassLoader child = new URLClassLoader(new URL[0], parent);
        final ChainingClassLoader chainingClassLoader = new ChainingClassLoader(parent);
        assertThat(chainingClassLoader.getClassLoaders())
                .hasSize(1)
                .containsExactly(parent);

        chainingClassLoader.addClassLoader(child);

        assertThat(chainingClassLoader.getClassLoaders())
                .hasSize(2)
                .containsExactly(parent, child);
    }

    public static final class Dummy {
    }
}