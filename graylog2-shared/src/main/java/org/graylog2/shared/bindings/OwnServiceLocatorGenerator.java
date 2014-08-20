/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.shared.bindings;

import com.google.inject.Injector;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.jvnet.hk2.external.generator.ServiceLocatorGeneratorImpl;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;
import org.jvnet.hk2.internal.*;

import javax.inject.Singleton;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class OwnServiceLocatorGenerator extends ServiceLocatorGeneratorImpl implements ServiceLocatorGenerator {
    private final Injector injector;
    public OwnServiceLocatorGenerator(Injector injector) {
        super();
        this.injector = injector;
    }

    private ServiceLocator initialize(String name, ServiceLocator parent) {
        if (parent != null && !(parent instanceof ServiceLocatorImpl)) {
            throw new AssertionError("parent must be a " + ServiceLocatorImpl.class.getName() +
                    " instead it is a " + parent.getClass().getName());
        }

        //ServiceLocatorImpl sli = new OwnServiceLocator(name, (ServiceLocatorImpl) parent, injector);
        ServiceLocatorImpl sli = new ServiceLocatorImpl(name, (ServiceLocatorImpl) parent);

        DynamicConfigurationImpl dci = new DynamicConfigurationImpl(sli);

        // The service locator itself
        dci.bind(Utilities.getLocatorDescriptor(sli));

        // The injection resolver for three thirty
        dci.addActiveDescriptor(Utilities.getThreeThirtyDescriptor(sli));

        // The dynamic configuration utility
        dci.bind(BuilderHelper.link(DynamicConfigurationServiceImpl.class, false).
                to(DynamicConfigurationService.class).
                in(Singleton.class.getName()).
                localOnly().
                build());

        dci.bind(BuilderHelper.createConstantDescriptor(
                new DefaultClassAnalyzer(sli)));

        dci.commit();

        GuiceBridge.getGuiceBridge().initializeGuiceBridge(sli);
        GuiceIntoHK2Bridge guiceBridge = sli.getService(GuiceIntoHK2Bridge.class);
        guiceBridge.bridgeGuiceInjector(injector);

        return sli;
    }

    @Override
    public ServiceLocator create(String name, ServiceLocator parent) {
        return initialize(name, parent);
    }
}
