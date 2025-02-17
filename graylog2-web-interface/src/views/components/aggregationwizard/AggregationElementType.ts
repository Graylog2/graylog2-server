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
import type * as React from 'react';

import type { AggregationWidgetConfigBuilder } from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import type { WidgetConfigFormValues, WidgetConfigValidationErrors } from './WidgetConfigForm';

export type AggregationElement<T extends keyof WidgetConfigFormValues> = {
  sectionTitle?: string;
  title: string;
  key: keyof WidgetConfigFormValues;
  allowCreate: (formValues: WidgetConfigFormValues) => boolean;
  order: number;
  onRemove?: (index: number, formValues: WidgetConfigFormValues) => WidgetConfigFormValues;
  toConfig?: (
    formValues: WidgetConfigFormValues,
    currentConfigBuilder: AggregationWidgetConfigBuilder,
  ) => AggregationWidgetConfigBuilder;
  fromConfig?: (config: AggregationWidgetConfig) => Partial<WidgetConfigFormValues>;
  onCreate?: (formValues: WidgetConfigFormValues) => WidgetConfigFormValues;
  onDeleteAll?: (formValues: WidgetConfigFormValues) => WidgetConfigFormValues;
  isEmpty: (formValues: WidgetConfigFormValues[T]) => boolean;
  component: React.ComponentType<{
    config: AggregationWidgetConfig;
    onConfigChange: (newConfig: AggregationWidgetConfig) => void;
  }>;
  validate?: (formValues: WidgetConfigFormValues) => WidgetConfigValidationErrors;
};
