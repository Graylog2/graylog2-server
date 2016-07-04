/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
