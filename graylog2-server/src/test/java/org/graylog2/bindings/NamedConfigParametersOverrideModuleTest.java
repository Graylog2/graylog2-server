package org.graylog2.bindings;

import com.github.joschi.jadconfig.Parameter;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.junit.Test;

import java.util.List;

import static com.google.inject.name.Names.named;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NamedConfigParametersOverrideModuleTest {
    static class BaseConfig {
        @Parameter(value = "test_param", required = true)
        private String testParam;

        BaseConfig(String testParam) {
            this.testParam = testParam;
        }
    }

    static class SubConfigA extends BaseConfig {
        SubConfigA(String testParam) {
            super(testParam);
        }
    }

    static class SubConfigB extends SubConfigA {
        @Parameter(value = "test_param_2", required = true)
        private String testParam2;

        SubConfigB(String testParam, String testParam2) {
            super(testParam);
            this.testParam2 = testParam2;
        }
    }

    static class OtherConfig {
        @Parameter(value = "test_param", required = true)
        private String testParam;

        OtherConfig(String testParam) {
            this.testParam = testParam;
        }
    }

    @Test
    public void inheritedParameterBindsOnce() {
        var subConfigA = new SubConfigA("test_param_value");
        var subConfigB = new SubConfigB("test_param_value_2", "test_param_2_value");

        Injector injector = Guice.createInjector(
                new NamedConfigParametersOverrideModule(List.of(subConfigA, subConfigB), false)
        );

        var testParam = injector.getInstance(Key.get(String.class, named("test_param")));
        var testParam2 = injector.getInstance(Key.get(String.class, named("test_param_2")));

        assertEquals("test_param_value_2", testParam);
        assertEquals("test_param_2_value", testParam2);
    }

    @Test
    public void duplicateParameterThrowsException() {
        var subConfigA = new SubConfigA("test_param_value");
        var otherConfig = new OtherConfig("test_param_value_2");

        assertThrows(CreationException.class, () -> Guice.createInjector(
                new NamedConfigParametersOverrideModule(List.of(subConfigA, otherConfig), false)
        ));
    }
}
