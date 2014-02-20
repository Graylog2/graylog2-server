/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.filters;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.testng.annotations.Test;

import java.util.Map;

import static junit.framework.Assert.assertEquals;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class StaticFieldFilterTest {

    @Test
    public void testFilter() throws Exception {
        Message msg = new Message("hello", "junit", Tools.iso8601());

        FakeInput fakeInput = new FakeInput();
        fakeInput.addStaticField("foo", "bar");

        msg.setSourceInput(fakeInput);

        StaticFieldFilter filter = new StaticFieldFilter();
        filter.filter(msg, null);

        assertEquals("hello", msg.getMessage());
        assertEquals("junit", msg.getSource());
        assertEquals("bar", msg.getField("foo"));
    }

    @Test
    public void testFilterIsNotOverwritingExistingKeys() throws Exception {
        Message msg = new Message("hello", "junit", Tools.iso8601());
        msg.addField("foo", "IWILLSURVIVE");

        FakeInput fakeInput = new FakeInput();
        fakeInput.addStaticField("foo", "bar");

        msg.setSourceInput(fakeInput);

        StaticFieldFilter filter = new StaticFieldFilter();
        filter.filter(msg, null);

        assertEquals("hello", msg.getMessage());
        assertEquals("junit", msg.getSource());
        assertEquals("IWILLSURVIVE", msg.getField("foo"));
    }

    private class FakeInput extends MessageInput {

        @Override
        public void checkConfiguration() throws ConfigurationException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void launch() throws MisfireException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void stop() {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public boolean isExclusive() {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public String getName() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public String linkToDocs() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Map<String, Object> getAttributes() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

}
