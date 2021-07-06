/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import MessageCountRotationStrategyConfiguration from './MessageCountRotationStrategyConfiguration';
import MessageCountRotationStrategySummary from './MessageCountRotationStrategySummary';
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
  ],
}));
