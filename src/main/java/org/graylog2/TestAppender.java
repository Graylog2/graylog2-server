/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.graylog2.database.MongoBridge;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.graylog2.messagehandlers.gelf.SimpleGELFClientHandler;

/**
 *
 * @author XING\lennart.koopmann
 */
public class TestAppender implements Appender {

    public void addFilter(Filter filter) {}

    public Filter getFilter() {
        return null;
    }

    public void clearFilters() {}

    public void close() {}

    public void doAppend(LoggingEvent le) {
        try {
            if (le.getLevel() != Level.INFO) {
                GELFMessage msg = new GELFMessage();
                msg.setShortMessage((String) le.getMessage());
                msg.setFullMessage(le.getRenderedMessage());
                msg.setLevel(Tools.log4jLevelToSyslog(le.getLevel()));
                msg.setHost(Tools.getLocalHostname());
                msg.setFacility("graylog2-server");
                msg.setVersion("1.0");
                msg.addAdditionalData("_thread_name", le.getThreadName());
                msg.addAdditionalData("_original_level", le.getLevel().toString());

                MongoBridge m = new MongoBridge();
                m.insertGelfMessage(msg);
            }
        } catch (Exception e) {}
    }

    public String getName() {
        return "FOOOOO";
    }

    public void setErrorHandler(ErrorHandler eh) {}

    public ErrorHandler getErrorHandler() {
        return null;
    }

    public void setLayout(Layout layout) {}

    public Layout getLayout() {
        return null;
    }

    public void setName(String string) {}

    public boolean requiresLayout() {
        return false;
    }


}
