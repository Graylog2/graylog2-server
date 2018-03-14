/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import org.graylog2.plugin.BaseConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.github.joschi.jadconfig.ReflectionUtils.getAllFields;
import static com.github.joschi.jadconfig.ReflectionUtils.getFieldValue;
import static java.util.Locale.ENGLISH;
import static org.graylog2.configuration.GraylogDataDirImpl.dataDir;

/**
 * This module creates bindings for {@link Path} parameters annotated with {@link GraylogBinPath} and
 * {@link GraylogDataDir}.
 *
 * <p>Example {@link GraylogBinPath} usage:
 *
 * <pre>
 *   public class ScriptExec {
 *     &#064;Inject
 *     ScriptExec(<b>@GraylogBinPath</b> Path binPath, String scriptName) {
 *         this.scriptName = binPath.resolve(scriptName);
 *     }
 *   }</pre>
 *
 * <p>Example {@link GraylogDataDir} usage:
 *
 * <pre>
 *   public class Config {
 *     &#064;Parameter("message_journal_dir")
 *     <b>&#064;GraylogDataDir</b>
 *     private Path messageJournalDir = Paths.get("journal");
 *   }
 *
 *   public class Journal {
 *     &#064;Inject
 *     Journal(<b>@GraylogDataDir("message_journal_dir")</b> Path journalDir) {
 *         // ...
 *     }
 *   }</pre>
 */
public class ConfigurationPathParametersModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationPathParametersModule.class);

    private final Set<Object> configurationBeans;

    /**
     * Create a new {@link ConfigurationPathParametersModule} instance and add bindings for the instances in {@code beans}.
     *
     * @param configurationBeans A {@link Collection} containing all instances of the configuration beans which
     *              should be registered.
     */
    public ConfigurationPathParametersModule(final Collection configurationBeans) {
        this.configurationBeans = new HashSet<Object>(configurationBeans);
    }

    @Override
    protected void configure() {
        // The BIN_PATH parameter MUST have a value
        final Path binPath = getConfigValue(BaseConfiguration.BIN_PATH)
                .orElseThrow(() -> new IllegalStateException(BaseConfiguration.BIN_PATH + " configuration value is not defined"));

        // The DATA_DIR parameter MUST have a value
        final Path dataDir = getConfigValue(BaseConfiguration.DATA_DIR)
                .orElseThrow(() -> new IllegalStateException(BaseConfiguration.DATA_DIR + " configuration value is not defined"));

        LOG.debug("Binding {} parameter annotated with @GraylogBinPath to {}", Path.class.getCanonicalName(), binPath.toAbsolutePath());
        bind(Path.class).annotatedWith(GraylogBinPath.class).toInstance(binPath.toAbsolutePath());

        for (final Object bean : configurationBeans) {
            createDataDirBindings(bean, dataDir);
        }
    }

    /**
     * Find the value for the given parameter name in all configuration beans.
     *
     * @param parameterName the config parameter name
     * @return a non-empty optional if the parameter value has been found
     */
    private Optional<Path> getConfigValue(final String parameterName) {
        final String name = Objects.requireNonNull(parameterName);

        for (Object bean : configurationBeans) {
            final Field[] fields = getAllFields(bean.getClass());

            for (Field field : fields) {
                final Parameter parameter = field.getAnnotation(Parameter.class);

                if (parameter != null) {
                    // We are only interested in this one parameter field
                    if (!parameter.value().equals(name)) {
                        continue;
                    }

                    // We are only interested in Path fields
                    if (!TypeLiteral.get(field.getGenericType()).equals(TypeLiteral.get(Path.class))) {
                        continue;
                    }

                    try {
                        final Object value = getFieldValue(bean, field);

                        if (value == null) {
                            return Optional.empty();
                        } else {
                            if (value instanceof Path) {
                                return Optional.of((Path) value);
                            } else {
                                // This shouldn't happen because we filter for paths above
                                throw new IllegalStateException("Parameter value is not a Path");
                            }
                        }
                    } catch (IllegalAccessException e) {
                        LOG.warn("Couldn't bind \"{}\"", field.getName(), e);
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Creates bindings for all {@link Path} configuration parameters that are annotated with {@link GraylogDataDir}.
     *
     * @param bean
     * @param dataDir
     */
    private void createDataDirBindings(Object bean, Path dataDir) {
        final Field[] fields = getAllFields(bean.getClass());

        for (Field field : fields) {
            final Parameter parameter = field.getAnnotation(Parameter.class);
            final GraylogDataDir dataDirAnnotation = field.getAnnotation(GraylogDataDir.class);

            // Only bind to config parameters which are annotated with GraylogDataDir
            if (parameter != null && dataDirAnnotation != null) {
                try {
                    // We are only interested in Path fields
                    if (!TypeLiteral.get(field.getGenericType()).equals(TypeLiteral.get(Path.class))) {
                        continue;
                    }

                    final Object value = getFieldValue(bean, field);
                    final GraylogDataDirImpl graylogDataDir = dataDir(parameter.value());

                    if (value instanceof Path) {
                        Path pathValue = (Path) value;
                        if (!pathValue.isAbsolute()) {
                            pathValue = dataDir.resolve(pathValue);
                        }
                        LOG.debug("Binding {} parameter annotated with {} to {}", Path.class.getCanonicalName(), graylogDataDir, pathValue.toAbsolutePath());
                        bind(Path.class).annotatedWith(graylogDataDir).toInstance(pathValue.toAbsolutePath());
                    } else {
                        final String valueString = value == null ? "null" : String.format(ENGLISH, "%s (%s)", value.toString(), value.getClass().getCanonicalName());
                        final String msg = String.format(ENGLISH, "Invalid value for %s: %s", graylogDataDir, valueString);
                        throw new IllegalStateException(msg);
                    }
                } catch (IllegalAccessException e) {
                    LOG.warn("Couldn't bind \"{}\"", field.getName(), e);
                }
            } else {
                LOG.debug("Skipping field \"{}\"", field.getName());
            }
        }
    }
}
