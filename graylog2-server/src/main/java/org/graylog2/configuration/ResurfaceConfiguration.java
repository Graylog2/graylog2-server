package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.converters.StringSetConverter;
import com.github.joschi.jadconfig.converters.URLConverter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.Set;

/**
 * Configuration class for API reporting to resurface.
 */
public class ResurfaceConfiguration {

    @Parameter(value = "resurface_url", converter = URLConverter.class)
    private URL resurfaceUrl;

    {
        try {
            resurfaceUrl = new URL("http://192.168.1.16:7701/message");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<URL> getResurfaceUrl() {
        return Optional.ofNullable(resurfaceUrl);
    }
}
