package org.graylog2.restclient.models;

import org.graylog2.restclient.lib.Version;

import java.net.URL;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class Plugin {
    public String unique_id;
    public String name;
    public String author;
    public URL url;
    public Version version;
    public String description;
    public Version required_version;
}
