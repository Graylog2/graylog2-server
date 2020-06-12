package org.graylog.plugins.customization;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class ThemeMode {
    private final static String THEME_MODE = "theme_mode";

    @JsonProperty(THEME_MODE)
    @Nullable
    public abstract String themeMode();

    @JsonCreator
    public static ThemeMode create(@JsonProperty(THEME_MODE) String themeMode) {
        return new AutoValue_ThemeMode(themeMode);
    }
}
