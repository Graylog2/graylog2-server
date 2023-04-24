/**
 * This package contains the AWS plugin, formerly located at https://github.com/Graylog2/graylog-plugin-aws.
 * <p>
 * The plugin intentionally resides in the same legacy package that was used when it was in the separate repository and
 * <em>not</em> underneath the {@code org.graylog.plugin} package. Changing the package would break migrations,
 * interaction with configuration classes and inputs, which use the old package name in identifiers.
 */
package org.graylog.aws;
