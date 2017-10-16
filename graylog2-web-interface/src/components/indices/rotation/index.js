import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import MessageCountRotationStrategyConfiguration from './MessageCountRotationStrategyConfiguration';
import MessageCountRotationStrategySummary from './MessageCountRotationStrategySummary';
import NoopRotationStrategyConfiguration from './NoopRotationStrategyConfiguration';
import NoopRotationStrategySummary from './NoopRotationStrategySummary';
import SizeBasedRotationStrategyConfiguration from './SizeBasedRotationStrategyConfiguration';
import SizeBasedRotationStrategySummary from './SizeBasedRotationStrategySummary';
import TimeBasedRotationStrategyConfiguration from './TimeBasedRotationStrategyConfiguration';
import TimeBasedRotationStrategySummary from './TimeBasedRotationStrategySummary';

PluginStore.register(new PluginManifest({}, {
  indexRotationConfig: [
    {
      type: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy',
      displayName: 'Index Message Count',
      configComponent: MessageCountRotationStrategyConfiguration,
      summaryComponent: MessageCountRotationStrategySummary,
    },
    {
      type: 'org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategy',
      displayName: 'Index Size',
      configComponent: SizeBasedRotationStrategyConfiguration,
      summaryComponent: SizeBasedRotationStrategySummary,
    },
    {
      type: 'org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy',
      displayName: 'Index Time',
      configComponent: TimeBasedRotationStrategyConfiguration,
      summaryComponent: TimeBasedRotationStrategySummary,
    },
    {
      type: 'org.graylog2.indexer.rotation.strategies.NoopRotationStrategy',
      displayName: 'Do nothing',
      configComponent: NoopRotationStrategyConfiguration,
      summaryComponent: NoopRotationStrategySummary,
    },
  ],
}));
