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
import { useState } from 'react';
import styled from 'styled-components';
import { isEmpty } from 'lodash';
import { EditWidgetComponentProps } from 'views/types';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';

import AggregationElementSelect from './AggregationElementSelect';
import ElementConfigurationContainer from './elementConfiguration/ElementConfigurationContainer';
import VisualizationConfiguration from './elementConfiguration/VisualizationConfiguration';
import GroupByConfiguration from './elementConfiguration/GroupByConfiguration';
import MetricConfiguration from './elementConfiguration/MetricConfiguration';
import SortConfiguration from './elementConfiguration/SortConfiguration';

export type CreateAggregationElement = (config: AggregationWidgetConfig, onConfigChange: (newConfig: AggregationWidgetConfig) => void) => AggregationElement;

export type AggregationElement = {
  title: string,
  key: string,
  isConfigured: boolean,
  multipleUse: boolean
  onCreate: () => void,
  onDeleteAll?: () => void,
  component: React.ComponentType<{
    config: AggregationWidgetConfig,
    onConfigChange: (newConfig: AggregationWidgetConfig) => void
  }>,
}

const createVisualizationElement: CreateAggregationElement = (config, onConfigChange) => ({
  title: 'Visualization',
  key: 'visualization',
  multipleUse: false,
  isConfigured: !isEmpty(config.visualization),
  onCreate: () => onConfigChange(config),
  component: VisualizationConfiguration,
});

const createMetricElement: CreateAggregationElement = (config, onConfigChange) => ({
  title: 'Metric',
  key: 'metric',
  multipleUse: true,
  isConfigured: !isEmpty(config.series),
  onCreate: () => onConfigChange(config.toBuilder().series([Series.createDefault()]).build()),
  onDeleteAll: () => onConfigChange(config.toBuilder().series([]).build()),
  component: MetricConfiguration,
});

const createGroupByElement: CreateAggregationElement = (config, onConfigChange) => ({
  title: 'Group By',
  key: 'groupBy',
  multipleUse: true,
  isConfigured: !isEmpty(config.rowPivots) || !isEmpty(config.columnPivots),
  onCreate: () => onConfigChange(config),
  onDeleteAll: () => onConfigChange(config.toBuilder().rowPivots([]).columnPivots([]).build()),
  component: GroupByConfiguration,
});

const createSortElement: CreateAggregationElement = (config, onConfigChange) => ({
  title: 'Sort',
  key: 'sort',
  multipleUse: false,
  isConfigured: !isEmpty(config.sort),
  onCreate: () => onConfigChange(config),
  onDeleteAll: () => onConfigChange(config.toBuilder().sort([]).build()),
  component: SortConfiguration,
});

const _createAggregationElements: (
  config: AggregationWidgetConfig,
  onConfigChange: (newConfig: AggregationWidgetConfig) => void
) => Array<AggregationElement> = (config, onConfigChange) => ([
  createVisualizationElement(config, onConfigChange),
  createGroupByElement(config, onConfigChange),
  createMetricElement(config, onConfigChange),
  createSortElement(config, onConfigChange),
]);

const _initialConfiguredAggregationElements = (aggregationElements: Array<AggregationElement>) => {
  return aggregationElements.reduce((configuredElements, element) => {
    if (element.isConfigured) {
      configuredElements.push(element.key);
    }

    return configuredElements;
  }, []);
};

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

const Section = styled.div`
  margin-bottom: 10px;

  :last-child {
    margin-bottom: 0;
  }
`;

const SectionHeadline = styled.div`
  margin-bottom: 5px;
`;

const AggregationWizard = ({ onChange, config, children }: EditWidgetComponentProps<AggregationWidgetConfig>) => {
  const aggregationElements = _createAggregationElements(config, onChange);
  const [configuredAggregationElements, setConfiguredAggregationElements] = useState(_initialConfiguredAggregationElements(aggregationElements));

  const _onElementCreate = (elementKey: string) => {
    if (elementKey) {
      setConfiguredAggregationElements([...configuredAggregationElements, elementKey]);
    }
  };

  return (
    <Wrapper>
      <Controls>
        <Section data-testid="add-element-section">
          <SectionHeadline>Add an Element</SectionHeadline>
          <AggregationElementSelect onElementCreate={_onElementCreate}
                                    aggregationElements={aggregationElements} />
        </Section>
        <Section data-testid="configure-elements-section">
          <SectionHeadline>Configured Elements</SectionHeadline>
          <div>
            {configuredAggregationElements.map((elementKey) => {
              const aggregationElement = aggregationElements.find((element) => element.key === elementKey);
              const AggregationElementComponent = aggregationElement.component;

              const onDeleteAll = () => {
                if (typeof aggregationElement.onDeleteAll !== 'function') {
                  return;
                }

                const newConfiguredElements = configuredAggregationElements.filter((element) => element !== aggregationElement.key);

                setConfiguredAggregationElements(newConfiguredElements);

                if (aggregationElement.isConfigured) {
                  aggregationElement.onDeleteAll();
                }
              };

              return (
                <ElementConfigurationContainer title={aggregationElement.title}
                                               onDeleteAll={onDeleteAll}
                                               isPermanentElement={aggregationElement.onDeleteAll === undefined}
                                               key={aggregationElement.key}>
                  <AggregationElementComponent config={config} onConfigChange={onChange} />
                </ElementConfigurationContainer>
              );
            })}
          </div>
        </Section>
      </Controls>
      <Visualization>
        {children}
      </Visualization>
    </Wrapper>
  );
};

export default AggregationWizard;
