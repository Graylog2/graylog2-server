package org.graylog2.outputs;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Retention(RetentionPolicy.RUNTIME)
@BindingAnnotation
public @interface DefaultMessageOutput {
}
