package org.graylog.collectors.db;

import org.graylog.collectors.config.receiver.CollectorReceiverConfig;
import org.graylog.collectors.config.receiver.MacOSUnifiedLoggingReceiverConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MacOSUnifiedLoggingSourceConfigTest {

    @Test
    void validate() {
        final MacOSUnifiedLoggingSourceConfig zeroPollInterval = MacOSUnifiedLoggingSourceConfig.builder()
                .maxPollInterval(Duration.ZERO)
                .maxLogAge(Duration.ZERO)
                .build();
        assertThatThrownBy(zeroPollInterval::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max_poll_interval");

        final MacOSUnifiedLoggingSourceConfig negativePollInterval = MacOSUnifiedLoggingSourceConfig.builder()
                .maxPollInterval(Duration.ofSeconds(-1))
                .maxLogAge(Duration.ZERO)
                .build();
        assertThatThrownBy(negativePollInterval::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max_poll_interval");


        final MacOSUnifiedLoggingSourceConfig negativeLogAge = MacOSUnifiedLoggingSourceConfig.builder()
                .maxPollInterval(Duration.ofSeconds(1))
                .maxLogAge(Duration.ofSeconds(-1))
                .build();
        assertThatThrownBy(negativeLogAge::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max_log_age");

    }

    @Test
    void toReceiverConfig() {
        final Optional<CollectorReceiverConfig> receiverConfigOpt = MacOSUnifiedLoggingSourceConfig.builder()
                .predicate("")
                .maxLogAge(Duration.ZERO)
                .maxPollInterval(Duration.ofSeconds(1))
                .build()
                .toReceiverConfig("test-1");

        assertThat(receiverConfigOpt).isNotEmpty();
        final CollectorReceiverConfig receiverConfig = receiverConfigOpt.get();
        assertThat(receiverConfig).isInstanceOf(MacOSUnifiedLoggingReceiverConfig.class);
        final MacOSUnifiedLoggingReceiverConfig macConfig  = (MacOSUnifiedLoggingReceiverConfig) receiverConfig;
        assertThat(macConfig.maxLogAge()).isEqualTo(Duration.ZERO);
        assertThat(macConfig.maxPollInterval()).isEqualTo(Duration.ofSeconds(1));
        assertThat(macConfig.predicate()).isNull();
    }
}
