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
/*
 * Created by IntelliJ IDEA.
 * User: kroepke
 * Date: 07/10/14
 * Time: 12:39
 */
package org.graylog2.inputs.codecs;

import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.inputs.codecs.Codec;

public class CodecsModule extends Graylog2Module {
    @Override
    protected void configure() {
        final MapBinder<String, Codec.Factory<? extends Codec>> mapBinder = codecMapBinder();

        // Aggregators must be singletons because codecs are instantiated in DecodingProcessor per message!
        bind(GelfChunkAggregator.class).in(Scopes.SINGLETON);

        installCodec(mapBinder, RawCodec.class);
        installCodec(mapBinder, SyslogCodec.class);
        installCodec(mapBinder, RandomHttpMessageCodec.class);
        installCodec(mapBinder, GelfCodec.class);
        installCodec(mapBinder, JsonPathCodec.class);
    }
}
