package org.graylog.datanode.shared.syste.activities;

import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataNodeActivityWriter implements ActivityWriter {
    private static final Logger LOG = LoggerFactory.getLogger(DataNodeActivityWriter.class);

    @Override
    public void write(Activity activity) {
        LOG.debug("Activity: {}", activity);
    }
}
