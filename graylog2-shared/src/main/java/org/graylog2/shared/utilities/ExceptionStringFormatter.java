package org.graylog2.shared.utilities;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ExceptionStringFormatter {
    private final Throwable throwable;

    public ExceptionStringFormatter(Throwable throwable) {
        this.throwable = throwable;
    }

    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
