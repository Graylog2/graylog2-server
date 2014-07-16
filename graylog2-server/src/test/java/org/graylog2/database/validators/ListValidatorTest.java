/**
 * Copyright 2013 Kay Roepke <kay@torch.sh>
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
package org.graylog2.database.validators;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

public class ListValidatorTest {
    @Test
    public void testValidate() throws Exception {
        final ListValidator v = new ListValidator();

        assertFalse(v.validate(null));
        assertFalse(v.validate(Maps.newHashMap()));
        assertTrue(v.validate(Lists.newArrayList()));
        assertTrue(v.validate(Lists.newArrayList("a", "string")));
    }
}
