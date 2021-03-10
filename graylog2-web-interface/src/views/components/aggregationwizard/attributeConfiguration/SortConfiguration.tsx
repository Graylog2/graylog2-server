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
import { isEmpty } from 'lodash';
import styled from 'styled-components';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import AttributeConfigurationContainer from './AttributeConfigurationContainer';

import type { CreateAggregationAttribute } from '../AggregationWizard';

export const createAggregationAttribute: CreateAggregationAttribute = (config, onConfigChange) => ({
  label: 'Sort',
  value: 'sort',
  isAvailable: isEmpty(config.sort),
  onCreate: (config) => {},
  onDeleteAll: (config) => {},
});

type Props = {
  config: AggregationWidgetConfig,
  onConfigChange: (newConfig: AggregationWidgetConfig) => void
}

const SortConfiguration = ({ config, onConfigChange }: Props) => {
  const aggregationAttribute = createAggregationAttribute(config, onConfigChange);

  return (
    <AttributeConfigurationContainer aggregationAttribute={aggregationAttribute}>
      Configuration Elements
    </AttributeConfigurationContainer>
  );
};

export default SortConfiguration;
