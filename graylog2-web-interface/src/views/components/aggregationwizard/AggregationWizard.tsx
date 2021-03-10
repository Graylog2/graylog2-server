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
import * as Immutable from 'immutable';
import styled from 'styled-components';
import { isEmpty } from 'lodash';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

import AggregationAttributeSelect from './AggregationAttributeSelect';
import VisualizationConfiguration from './attributeConfiguration/VisualizationConfiguration';
import GroupByConfiguration from './attributeConfiguration/GroupByConfiguration';
import MetricConfiguration from './attributeConfiguration/MetricConfiguration';
import SortConfiguration from './attributeConfiguration/SortConfiguration';

export type CreateAggregationAttribute = (config: AggregationWidgetConfig, onConfigChange: (newConfig: AggregationWidgetConfig) => void) => AggregationAttribute;

export type AggregationAttribute = {
  label: string,
  value: string,
  isAvailable: boolean,
  onCreate: () => void,
  onDeleteAll: () => void,
}

const Wrapper = styled.div`
  height: 100%;
  display: flex;
`;

const Controls = styled.div`
  height: 100%;
  min-width: 300px;
  max-width: 500px;
  flex: 1;
`;

const Visualization = styled.div`
  height: 100%;
  flex: 3;
`;

export type Props = {
  children: React.ReactNode,
  config: AggregationWidgetConfig,
  fields: Immutable.List<FieldTypeMapping>,
  onChange: (newConfig: AggregationWidgetConfig) => void,
};

const AggregationWizard = ({ onChange, config, children }: Props) => {
  return (
    <Wrapper>
      <Controls>
        <AggregationAttributeSelect onConfigChange={onChange}
                                    config={config} />
        <div>
          {!isEmpty(config.visualization) && (
            <VisualizationConfiguration config={config} onConfigChange={onChange} />
          )}
          {(!isEmpty(config.rowPivots) || !isEmpty(config.columnPivots)) && (
            <GroupByConfiguration config={config} onConfigChange={onChange} />
          )}
          {!isEmpty(config.series) && (
            <MetricConfiguration config={config} onConfigChange={onChange} />
          )}
          {!isEmpty(config.sort) && (
            <SortConfiguration config={config} onConfigChange={onChange} />
          )}
        </div>
      </Controls>
      <Visualization>
        {children}
      </Visualization>
    </Wrapper>
  );
};

export default AggregationWizard;
