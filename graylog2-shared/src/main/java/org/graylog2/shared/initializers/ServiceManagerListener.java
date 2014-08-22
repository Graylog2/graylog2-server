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
package org.graylog2.shared.initializers;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager.Listener;
import org.graylog2.plugin.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ServiceManagerListener extends Listener {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceManagerListener.class);
    private final ServerStatus serverStatus;

    @Inject
    public ServiceManagerListener(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    @Override
    public void healthy() {
        LOG.info("Services are healthy");
        serverStatus.start();
    }

    @Override
    public void stopped() {
        LOG.info("Services are now stopped.");
    }

    @Override
    public void failure(Service service) {
        // do not log the failure here again, the ServiceManager itself does so already on Level ERROR.
        serverStatus.fail();
    }
}
