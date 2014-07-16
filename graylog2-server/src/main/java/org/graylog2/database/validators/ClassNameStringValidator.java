/*
 * Copyright 2012-2014 TORCH GmbH
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
 */

package org.graylog2.database.validators;

import org.apache.commons.lang.ArrayUtils;
import org.graylog2.plugin.database.validators.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ClassNameStringValidator implements Validator {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    private final Class<?> classConstraint;

    public ClassNameStringValidator(Class<?> classConstraint) {
        this.classConstraint = classConstraint;
    }

    @Override
    public boolean validate(Object value) {
        if (!(value instanceof String))
            return false;

        final String className = (String) value;
        final Class<?> classToCheck;
        try {
            classToCheck = Class.forName(className);
        } catch (ClassNotFoundException | ClassCastException e) {
            LOG.error("String {} does not seem to be a valid class name for class {}: {}", className, classConstraint, e);
            return false;
        }

        for (Class<?> intf : classToCheck.getInterfaces()) {
            if (intf.equals(classConstraint))
                return true;
        }

        LOG.error("Class {} does not implement interface {}!", classToCheck.getCanonicalName(), classConstraint.getCanonicalName());
        return false;
    }
}
