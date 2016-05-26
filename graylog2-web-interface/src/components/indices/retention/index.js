import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import DeletionRetentionStrategyConfiguration from './DeletionRetentionStrategyConfiguration';
import DeletionRetentionStrategySummary from './DeletionRetentionStrategySummary';
import ClosingRetentionStrategyConfiguration from './ClosingRetentionStrategyConfiguration';
import ClosingRetentionStrategySummary from './ClosingRetentionStrategySummary';
import NoopRetentionStrategyConfiguration from './NoopRetentionStrategyConfiguration';
import NoopRetentionStrategySummary from './NoopRetentionStrategySummary';

PluginStore.register(new PluginManifest({}, {
  indexRetentionConfig: [
    {
      type: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy',
      displayName: 'Delete Index',
      configComponent: DeletionRetentionStrategyConfiguration,
      summaryComponent: DeletionRetentionStrategySummary,
    },
    {
      type: 'org.graylog2.indexer.retention.strategies.ClosingRetentionStrategy',
      displayName: 'Close Index',
      configComponent: ClosingRetentionStrategyConfiguration,
      summaryComponent: ClosingRetentionStrategySummary,
    },
    {
      type: 'org.graylog2.indexer.retention.strategies.NoopRetentionStrategy',
      displayName: 'Do nothing',
      configComponent: NoopRetentionStrategyConfiguration,
      summaryComponent: NoopRetentionStrategySummary,
    },
  ],
}));
