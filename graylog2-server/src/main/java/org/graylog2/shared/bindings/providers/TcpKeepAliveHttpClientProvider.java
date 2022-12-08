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
package org.graylog2.shared.bindings.providers;

import com.google.common.base.Suppliers;
import okhttp3.OkHttpClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.SocketFactory;
import java.util.function.Supplier;

@Singleton
public class TcpKeepAliveHttpClientProvider {

    private final Supplier<OkHttpClient> supplier;

    @Inject
    public TcpKeepAliveHttpClientProvider(OkHttpClientProvider provider) {
        this.supplier = Suppliers.memoize(() -> provider.get().newBuilder()
                .socketFactory(new TcpKeepAliveSocketFactory(SocketFactory.getDefault()))
                .build());
    }

    public OkHttpClient get() {
        return supplier.get();
    }
}
