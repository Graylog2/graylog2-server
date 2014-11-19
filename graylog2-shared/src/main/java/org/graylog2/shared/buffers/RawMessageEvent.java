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
package org.graylog2.shared.buffers;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventTranslatorOneArg;
import org.graylog2.plugin.journal.RawMessage;

public class RawMessageEvent {

    public RawMessage rawMessage;

    public byte[] encodedRawMessage;

    public static final EventFactory<RawMessageEvent> FACTORY = new EventFactory<RawMessageEvent>() {
        @Override
        public RawMessageEvent newInstance() {
            return new RawMessageEvent();
        }
    };
    public static final EventTranslatorOneArg<RawMessageEvent, RawMessage> TRANSLATOR = new EventTranslatorOneArg<RawMessageEvent, RawMessage>() {
        @Override
        public void translateTo(RawMessageEvent event, long sequence, RawMessage arg0) {
            event.rawMessage = arg0;
        }
    };
}
