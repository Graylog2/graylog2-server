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
package org.graylog2.plugin;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public abstract class AbstractDescriptor {
    private final String name;
    private final boolean exclusive;
    private final String linkToDocs;

    // required for guice, but isn't called.
    protected AbstractDescriptor() {
        throw new IllegalStateException("This class should not be instantiated directly, this is a bug.");
    }

    protected AbstractDescriptor(String name, boolean exclusive, String linkToDocs) {
        this.name = name;
        this.exclusive = exclusive;
        this.linkToDocs = linkToDocs;
    }

    public String getName() {
        return name;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public String getLinkToDocs() {
        return linkToDocs;
    }
}
