package org.graylog2;

import java.io.File;

import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;

public class FilePresentValidator implements Validator {

    @Override
    public void validate(String name, String value)
            throws ValidationException {
        File file = new File(value);
        if (file.canRead()) {
            return;
        }
        throw new ValidationException("Cannot read file " + name + " at path " + value +" . Please specify the correct path or change the permissions");
    }

}