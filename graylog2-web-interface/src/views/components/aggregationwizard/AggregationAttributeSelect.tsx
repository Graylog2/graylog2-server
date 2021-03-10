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
import * as React from 'react';
import styled from 'styled-components';
import { isEmpty } from 'lodash';

import { Select } from 'components/common';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import { createAggregationAttribute as createVisualizationAttribute } from './attributeConfiguration/VisualizationConfiguration';
import { createAggregationAttribute as createGroupByAttribute } from './attributeConfiguration/GroupByConfiguration';
import { createAggregationAttribute as createMetricAttribute } from './attributeConfiguration/MetricConfiguration';
import { createAggregationAttribute as createSortAttribute } from './attributeConfiguration/SortConfiguration';
import type { AggregationAttribute } from './AggregationWizard';

const _createAggregationAttributes: (
  config: AggregationWidgetConfig,
  onConfigChange: (newConfig: AggregationWidgetConfig) => void
) => Array<AggregationAttribute> = (config, onConfigChange) => ([
  createVisualizationAttribute(config, onConfigChange),
  createGroupByAttribute(config, onConfigChange),
  createMetricAttribute(config, onConfigChange),
  createSortAttribute(config, onConfigChange),
]);

const Wrapper = styled.div`
  margin-bottom: 10px;
`;

const _getAvailableAttributes = (config: AggregationWidgetConfig, onConfigChange: (newConfig: AggregationWidgetConfig) => void) => {
  const aggregationAttributes = _createAggregationAttributes(config, onConfigChange);

  return aggregationAttributes.filter((option) => option.isAvailable);
};

type Props = {
  config: AggregationWidgetConfig,
  onConfigChange: (newConfig: AggregationWidgetConfig) => void,
}

const AggregationAttributeSelect = ({ config, onConfigChange }: Props) => {
  const aggregationAttributes = _getAvailableAttributes(config, onConfigChange);

  const _onAttributeSelect = (attributeValue) => {
    const selectedAttribute = aggregationAttributes.find((attribute) => attribute.value === attributeValue);
    selectedAttribute.onCreate();
  };

  return (
    <Wrapper>
      <Select options={aggregationAttributes} onChange={_onAttributeSelect} />
    </Wrapper>
  );
};

export default AggregationAttributeSelect;
