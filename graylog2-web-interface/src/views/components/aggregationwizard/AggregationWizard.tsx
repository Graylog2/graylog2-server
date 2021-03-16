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
import { EditWidgetComponentProps } from 'views/types';

import Series, { parseSeries } from 'views/logic/aggregationbuilder/Series';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';
import { ButtonToolbar } from 'components/graylog';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Button from 'components/graylog/Button';

import WidgetConfigForm, { WidgetConfigFormValues, MetricFormValues } from './WidgetConfigForm';
import AggregationElementSelect from './AggregationElementSelect';
import ElementConfigurationContainer from './elementConfiguration/ElementConfigurationContainer';
import VisualizationConfiguration from './elementConfiguration/VisualizationConfiguration';
import GroupByConfiguration from './elementConfiguration/GroupByConfiguration';
import MetricsConfiguration from './elementConfiguration/MetricsConfiguration';
import SortConfiguration from './elementConfiguration/SortConfiguration';

export type AggregationElement = {
  title: string,
  key: string,
  isConfigured: (formValues: WidgetConfigFormValues) => boolean,
  multipleUse: boolean,
  order: number,
  toConfig?: (formValues: WidgetConfigFormValues, currentConfig: AggregationWidgetConfig) => AggregationWidgetConfig,
  fromConfig?: (config: AggregationWidgetConfig) => Partial<WidgetConfigFormValues>,
  onCreate?: () => void,
  onDeleteAll?: (formValues: WidgetConfigFormValues) => WidgetConfigFormValues,
  component: React.ComponentType<{
    config: AggregationWidgetConfig,
    onConfigChange: (newConfig: AggregationWidgetConfig) => void
  }>,
}

const _metricsToSeries = (formMetrics: Array<MetricFormValues>) => formMetrics
  .map((metric) => Series.create(metric.function, metric.field)
    .toBuilder()
    .config(SeriesConfig.empty().toBuilder().name(metric.name).build())
    .build());

const _seriesToMetrics = (series: Array<Series>) => series.map((s) => {
  const { type: func, field } = parseSeries(s.function);

  return {
    function: func,
    field,
    name: s.config?.name,
  };
});

const visualizationElement: AggregationElement = {
  title: 'Visualization',
  key: 'visualization',
  order: 4,
  multipleUse: false,
  isConfigured: (formValues: WidgetConfigFormValues) => !isEmpty(formValues.visualization),
  component: VisualizationConfiguration,
};

const metricElement: AggregationElement = {
  title: 'Metric',
  key: 'metrics',
  order: 2,
  multipleUse: true,
  isConfigured: (formValues: WidgetConfigFormValues) => !isEmpty(formValues.metrics),
  fromConfig: (providedConfig: AggregationWidgetConfig) => ({
    metrics: _seriesToMetrics(providedConfig.series),
  }),
  toConfig: (formValues: WidgetConfigFormValues, currentConfig: AggregationWidgetConfig) => currentConfig
    .toBuilder()
    .series(_metricsToSeries(formValues.metrics))
    .build(),
  component: MetricsConfiguration,
};

const groupByElement: AggregationElement = {
  title: 'Group By',
  key: 'groupBy',
  order: 1,
  multipleUse: true,
  isConfigured: (formValues: WidgetConfigFormValues) => !isEmpty(formValues.groupBy),
  component: GroupByConfiguration,
};

const sortElement: AggregationElement = {
  title: 'Sort',
  key: 'sort',
  order: 3,
  multipleUse: false,
  isConfigured: (formValues: WidgetConfigFormValues) => !isEmpty(formValues.sort),
  component: SortConfiguration,
};

const aggregationElements = [
  visualizationElement,
  metricElement,
  sortElement,
  groupByElement,
];

const aggregationElementsByKey = Object.fromEntries(aggregationElements.map((element) => ([element.key, element])));

const _initialFormValues = (config: AggregationWidgetConfig) => {
  return aggregationElements.reduce((formValues, element) => ({
    ...formValues,
    ...(element.fromConfig ? element.fromConfig(config) : {}),
  }), {});
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
  const initialFormValues = _initialFormValues(config);

  const _onSubmit = (formValues: WidgetConfigFormValues) => {
    const toConfigByKey = Object.fromEntries(aggregationElements.map(({ key, toConfig }) => [key, toConfig]));

    const newConfig = Object.keys(formValues).map((key) => {
      const toConfig = toConfigByKey[key] ?? ((_values, prevConfig) => prevConfig);

      if (!toConfig) {
        throw new Error(`Aggregation element with key ${key} is missing toConfig.`);
      }

      return toConfig;
    }).reduce((prevConfig, toConfig) => toConfig(formValues, prevConfig), AggregationWidgetConfig.builder().build());

    onChange(newConfig);
  };

  return (
    <Wrapper>
      <Controls>

        <WidgetConfigForm onSubmit={_onSubmit} initialValues={initialFormValues}>
          {({ isValid, dirty, values, setValues }) => {
            const _onElementCreate = (elementKey: string) => {
              setValues({
                ...values,
                [elementKey]: [
                  ...(values[elementKey] ?? []),
                  {},
                ],
              });
            };

            return (
              <>
                <Section data-testid="add-element-section">
                  <SectionHeadline>Add an Element</SectionHeadline>
                  <AggregationElementSelect onElementCreate={_onElementCreate}
                                            aggregationElements={aggregationElements} />
                </Section>
                <Section data-testid="configure-elements-section">
                  <SectionHeadline>Configured Elements</SectionHeadline>
                  <div>
                    {Object.keys(values).sort((elementKey1, elementKey2) => aggregationElementsByKey[elementKey1].order - aggregationElementsByKey[elementKey2].order).map((elementKey) => {
                      const aggregationElement = aggregationElementsByKey[elementKey];

                      if (!aggregationElement) {
                        throw new Error(`Aggregation element with key ${elementKey} is missing but configured for this widget.`);
                      }

                      const AggregationElementComponent = aggregationElement.component;

                      const onDeleteAll = () => {
                        if (typeof aggregationElement.onDeleteAll !== 'function') {
                          return;
                        }

                        setValues(aggregationElement.onDeleteAll(values));
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
                  {dirty && (
                    <ButtonToolbar className="pull-right">
                      <Button type="submit" disabled={!isValid}>Apply</Button>
                    </ButtonToolbar>
                  )}
                </Section>
              </>
            );
          }}
        </WidgetConfigForm>
      </Controls>
      <Visualization>
        {children}
      </Visualization>
    </Wrapper>
  );
};

export default AggregationWizard;
