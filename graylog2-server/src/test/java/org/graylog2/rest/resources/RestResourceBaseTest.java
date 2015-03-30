package org.graylog2.rest.resources;

import com.google.inject.Module;
import org.assertj.core.util.Lists;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.Before;

import java.util.List;

public class RestResourceBaseTest {
    @Before
    public void setUpInjector() throws Exception {
        // The list of modules is empty for now so only JIT injection will be used.
        final List<Module> modules = Lists.emptyList();
        GuiceInjectorHolder.createInjector(modules);
    }
}
