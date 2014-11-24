/*
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

package org.graylog2.bootstrap;

import io.airlift.command.Cli;
import io.airlift.command.Cli.CliBuilder;
import io.airlift.command.Help;
import org.graylog2.bootstrap.commands.Radio;
import org.graylog2.bootstrap.commands.Server;
import org.graylog2.bootstrap.commands.ShowVersion;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class Main {
    public static void main(String[] argv) {
        CliBuilder<Runnable> builder = Cli.<Runnable>builder("graylog2")
                .withDescription("Open source, centralized log management")
                .withDefaultCommand(Help.class)
                .withCommands(Server.class, Radio.class, ShowVersion.class);

        Cli<Runnable> gitParser = builder.build();
        gitParser.parse(argv).run();
    }
}
