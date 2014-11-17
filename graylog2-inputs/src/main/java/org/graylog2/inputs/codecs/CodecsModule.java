/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Created by IntelliJ IDEA.
 * User: kroepke
 * Date: 07/10/14
 * Time: 12:39
 */
package org.graylog2.inputs.codecs;

import com.google.inject.multibindings.MapBinder;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.inputs.codecs.Codec;

public class CodecsModule extends Graylog2Module {
    protected void configure() {
        final MapBinder<String, Codec.Factory<? extends Codec>> mapBinder = codecMapBinder();

        installCodec(mapBinder, "raw", RawCodec.class);
        installCodec(mapBinder, "syslog", SyslogCodec.class);
        installCodec(mapBinder, "randomhttp", RandomHttpMessageCodec.class);
        installCodec(mapBinder, "gelf", GelfCodec.class);
        installCodec(mapBinder, "radio-msgpack", RadioMessageCodec.class);
        installCodec(mapBinder, "jsonpath", JsonPathCodec.class);
    }

}
