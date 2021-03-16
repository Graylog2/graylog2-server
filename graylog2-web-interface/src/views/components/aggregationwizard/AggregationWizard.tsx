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
import { useState, useMemo } from 'react';
import styled from 'styled-components';
import { isEmpty } from 'lodash';
import { EditWidgetComponentProps } from 'views/types';

import { defaultCompare } from 'views/logic/DefaultCompare';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

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
  multipleUse: boolean,
  order: number,
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
  order: 4,
  multipleUse: false,
  isConfigured: !isEmpty(config.visualization),
  onCreate: () => onConfigChange(config),
  component: VisualizationConfiguration,
});

const createMetricElement: CreateAggregationElement = (config, onConfigChange) => ({
  title: 'Metric',
  key: 'metric',
  order: 2,
  multipleUse: true,
  isConfigured: !isEmpty(config.series),
  onCreate: () => onConfigChange(config),
  onDeleteAll: () => onConfigChange(config.toBuilder().series([]).build()),
  component: MetricConfiguration,
});

const createGroupByElement: CreateAggregationElement = (config, onConfigChange) => ({
  title: 'Group By',
  key: 'groupBy',
  order: 1,
  multipleUse: true,
  isConfigured: !isEmpty(config.rowPivots) || !isEmpty(config.columnPivots),
  onCreate: () => onConfigChange(config),
  onDeleteAll: () => onConfigChange(config.toBuilder().rowPivots([]).columnPivots([]).build()),
  component: GroupByConfiguration,
});

const createSortElement: CreateAggregationElement = (config, onConfigChange) => ({
  title: 'Sort',
  key: 'sort',
  order: 3,
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
  createGroupByElement(config, onConfigChange),
  createMetricElement(config, onConfigChange),
  createSortElement(config, onConfigChange),
  createVisualizationElement(config, onConfigChange),
]);

const _initialConfiguredAggregationElements = (aggregationElements: Array<AggregationElement>) => {
  return aggregationElements.reduce((configuredElements, element) => {
    if (element.isConfigured) {
      configuredElements.push(element.key);
    }

    return configuredElements;
  }, []);
};

const _sortElements = (configuredAggregationElements: Array<string>, aggregationElements: Array<AggregationElement>) => {
  const sortedAggregationElements = aggregationElements.sort((element1, element2) => defaultCompare(element1.order, element2.order));

  return sortedAggregationElements.reduce((collection, element) => {
    if (configuredAggregationElements.find((elementKey) => elementKey === element.key)) {
      collection.push(element.key);
    }

    return collection;
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
  overflow-y: auto;
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
  const [configuredAggregationElements, setConfiguredAggregationElements] = useState<Array<string>>(_initialConfiguredAggregationElements(aggregationElements));
  const sortedConfiguredElements = useMemo(() => _sortElements(configuredAggregationElements, aggregationElements), [configuredAggregationElements, aggregationElements]);

  const _onElementCreate = (elementKey: string) => {
    if (elementKey && !configuredAggregationElements.find((configuredElementKey) => configuredElementKey === elementKey)) {
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
            {sortedConfiguredElements.map((elementKey) => {
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
