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

import org.graylog2.plugin.database.validators.ValidationResult;
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
    public ValidationResult validate(Object value) {
        if (!(value instanceof String)) {
            final String error = "Value " + value + " is not a String!";
            LOG.error(error);
            return new ValidationResult.ValidationFailed(error);
        }

        final String className = (String) value;
        final Class<?> classToCheck;
        try {
            classToCheck = Class.forName(className);
        } catch (ClassNotFoundException | ClassCastException e) {
            final String error = "String " + className + " does not seem to be a valid class name for class " + classConstraint + ": " + e;
            LOG.error(error);
            return new ValidationResult.ValidationFailed(error);
        }

        Class<?> currentClass = classToCheck;

        while(!currentClass.equals(Object.class)) {
            for (Class<?> intf : currentClass.getInterfaces()) {
                if (intf.equals(classConstraint))
                    return new ValidationResult.ValidationPassed();
            }

            currentClass = currentClass.getSuperclass();
        }

        final String error = "Class " + classToCheck.getCanonicalName() + " does not implement interface " + classConstraint.getCanonicalName() + "!";
        LOG.error(error);
        return new ValidationResult.ValidationFailed(error);
    }
}
