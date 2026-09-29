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
package org.graylog2.shared.inputs;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageInputFactoryTest {
    private static final String TYPE = "org.graylog2.inputs.FakeInput";
    private static final String OTHER_TYPE = "org.graylog2.inputs.OtherFakeInput";

    @Test
    void instantiatesAnInputOnlyOncePerTypeToAnswerOnlyOnePerCluster() throws Exception {
        final MessageInput.Factory<MessageInput> factory = factoryFor(true);
        final MessageInputFactory messageInputFactory = messageInputFactory(Map.of(TYPE, factory));

        assertThat(messageInputFactory.onlyOnePerCluster(TYPE, new Configuration(Map.of()))).isTrue();
        assertThat(messageInputFactory.onlyOnePerCluster(TYPE, new Configuration(Map.of()))).isTrue();

        verify(factory, times(1)).create(any());
    }

    @Test
    void cachesEachTypeSeparately() throws Exception {
        final MessageInput.Factory<MessageInput> factory = factoryFor(true);
        final MessageInput.Factory<MessageInput> otherFactory = factoryFor(false);
        final MessageInputFactory messageInputFactory =
                messageInputFactory(Map.of(TYPE, factory, OTHER_TYPE, otherFactory));

        assertThat(messageInputFactory.onlyOnePerCluster(TYPE, new Configuration(Map.of()))).isTrue();
        assertThat(messageInputFactory.onlyOnePerCluster(OTHER_TYPE, new Configuration(Map.of()))).isFalse();
        assertThat(messageInputFactory.onlyOnePerCluster(TYPE, new Configuration(Map.of()))).isTrue();

        verify(factory, times(1)).create(any());
        verify(otherFactory, times(1)).create(any());
    }

    @Test
    void instantiatesOnceWhenSeveralThreadsAskForTheSameTypeAtOnce() throws Exception {
        final int threads = 8;
        final CountDownLatch start = new CountDownLatch(1);
        final MessageInput messageInput = mock(MessageInput.class);
        when(messageInput.onlyOnePerCluster()).thenReturn(true);
        @SuppressWarnings("unchecked")
        final MessageInput.Factory<MessageInput> factory = mock(MessageInput.Factory.class);
        when(factory.create(any())).thenAnswer(i -> {
            Thread.sleep(50);
            return messageInput;
        });
        final MessageInputFactory messageInputFactory = messageInputFactory(Map.of(TYPE, factory));

        final ExecutorService pool = Executors.newFixedThreadPool(threads,
                new ThreadFactoryBuilder().setNameFormat("only-one-per-cluster-test-%d").build());
        try {
            final List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return messageInputFactory.onlyOnePerCluster(TYPE, new Configuration(Map.of()));
                }));
            }
            start.countDown();
            for (Future<Boolean> result : results) {
                assertThat(result.get(10, TimeUnit.SECONDS)).isTrue();
            }
        } finally {
            pool.shutdownNow();
        }

        verify(factory, times(1)).create(any());
    }

    @Test
    void buildsTheAvailableInputDescriptionsOnlyOnce() {
        final MessageInput.Factory<MessageInput> factory = factoryFor(true);
        final MessageInputFactory messageInputFactory = messageInputFactory(Map.of(TYPE, factory));

        final Map<String, InputDescription> first = messageInputFactory.getAvailableInputs();

        assertThat(messageInputFactory.getAvailableInputs()).isSameAs(first);
        verify(factory, times(1)).getDescriptor();
        verify(factory, times(1)).getConfig();
    }

    /**
     * The type answer is cached on the instance, so a per-injection instance would answer from an empty cache and
     * instantiate an input on every REST request. Resources are built per request, hence this test.
     */
    @Test
    void isASingletonSoTheCachedTypeAnswersOutliveOneInjection() {
        final Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(new TypeLiteral<Map<String, MessageInput.Factory<? extends MessageInput>>>() {})
                        .toInstance(Map.of());
                bind(FeatureFlags.class).toInstance(mock(FeatureFlags.class));
            }
        });

        assertThat(injector.getInstance(MessageInputFactory.class))
                .isSameAs(injector.getInstance(MessageInputFactory.class));
    }

    private MessageInputFactory messageInputFactory(Map<String, MessageInput.Factory<? extends MessageInput>> factories) {
        return new MessageInputFactory(factories, mock(FeatureFlags.class));
    }

    @SuppressWarnings("unchecked")
    private MessageInput.Factory<MessageInput> factoryFor(boolean onlyOnePerCluster) {
        final MessageInput messageInput = mock(MessageInput.class);
        when(messageInput.onlyOnePerCluster()).thenReturn(onlyOnePerCluster);
        final MessageInput.Factory<MessageInput> factory = mock(MessageInput.Factory.class);
        when(factory.create(any())).thenReturn(messageInput);
        return factory;
    }
}
