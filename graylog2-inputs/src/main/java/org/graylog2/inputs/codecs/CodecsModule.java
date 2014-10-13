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

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import org.graylog2.plugin.inputs.codecs.Codec;

public class CodecsModule extends AbstractModule {
    protected void configure() {
        install(new FactoryModuleBuilder().implement(Codec.class, RawCodec.class).build(RawCodec.Factory.class));
        install(new FactoryModuleBuilder().implement(Codec.class, SyslogCodec.class).build(SyslogCodec.Factory.class));
        install(new FactoryModuleBuilder().implement(Codec.class, RandomHttpMessageCodec.class).build(RandomHttpMessageCodec.Factory.class));
        install(new FactoryModuleBuilder().implement(Codec.class, GelfCodec.class).build(GelfCodec.Factory.class));
        install(new FactoryModuleBuilder().implement(Codec.class, RadioMessageCodec.class).build(RadioMessageCodec.Factory.class));

        final MapBinder<String, Codec.Factory<? extends Codec>> mapBinder =
                MapBinder.newMapBinder(binder(),
                                       TypeLiteral.get(String.class),
                                       new TypeLiteral<Codec.Factory<? extends Codec>>() {
                                       });

        mapBinder.addBinding("raw").to(Key.get(RawCodec.Factory.class));
        mapBinder.addBinding("syslog").to(Key.get(SyslogCodec.Factory.class));
        mapBinder.addBinding("randomhttp").to(Key.get(RandomHttpMessageCodec.Factory.class));
        mapBinder.addBinding("gelf").to(Key.get(GelfCodec.Factory.class));
        mapBinder.addBinding("radio-msgpack").to(Key.get(RadioMessageCodec.Factory.class));
    }
}
