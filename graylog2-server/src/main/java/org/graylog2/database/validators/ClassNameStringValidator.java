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
