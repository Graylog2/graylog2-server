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

import FilterAggregationFormContainer from './FilterAggregationFormContainer';
import FilterAggregationForm from './FilterAggregationForm';
import FilterAggregationSummary from './FilterAggregationSummary';

PluginStore.register(new PluginManifest({}, {
  eventDefinitionTypes: [
    {
      type: 'aggregation-v1',
      displayName: 'Filter & Aggregation',
      sortOrder: 0, // Sort before conditions working on events
      description: 'Create Events from log messages by filtering them and (optionally) '
        + 'aggregating their results to match a given condition. These Events can be used as input for a Correlation Rule.',
      formComponent: FilterAggregationFormContainer,
      summaryComponent: FilterAggregationSummary,
      defaultConfig: FilterAggregationForm.defaultConfig,
    },
  ],
}));
