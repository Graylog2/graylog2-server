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

import type { EditWidgetComponentProps } from 'views/types';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import type { WidgetConfigFormValues, WidgetConfigValidationErrors } from './WidgetConfigForm';
import WidgetConfigForm from './WidgetConfigForm';
import ElementsConfiguration from './ElementsConfiguration';
import aggregationElements from './aggregationElementDefinitions';
import VisualizationContainer from './VisualizationContainer';

const aggregationElementsByKey = Object.fromEntries(aggregationElements.map((element) => ([element.key, element])));

const _initialFormValues = (config: AggregationWidgetConfig) => {
  return aggregationElements.reduce((formValues, element) => ({
    ...formValues,
    ...(element.fromConfig ? element.fromConfig(config) : {}),
  }), {});
};

const Controls = styled.div`
  height: 100%;
  min-width: 315px;
  max-width: 500px;
  flex: 1.2;
  padding-right: 15px;
`;

const Section = styled.div`
  height: 100%;
  margin-bottom: 10px;

  :last-child {
    margin-bottom: 0;
  }
`;

const onCreateElement = (
  elementKey: string,
  values: WidgetConfigFormValues,
  setValues: (formValues: WidgetConfigFormValues) => void,
) => {
  const aggregationElement = aggregationElementsByKey[elementKey];

  if (aggregationElement?.onCreate) {
    setValues(aggregationElement.onCreate(values));
  } else {
    setValues({
      ...values,
      [elementKey]: [
        ...(values[elementKey] ?? []),
        {},
      ],
    });
  }
};

export const updateWidgetAggregationElements = (formValues: WidgetConfigFormValues, oldConfig = AggregationWidgetConfig.builder().build()) => {
  const toConfigByKey = Object.fromEntries(aggregationElements.map(({ key, toConfig }) => [key, toConfig]));

  const newConfig = Object.keys(formValues).map((key) => {
    const toConfig = toConfigByKey[key] ?? ((_values, prevConfig) => prevConfig);

    if (!toConfig) {
      throw new Error(`Aggregation element with key ${key} is missing toConfig.`);
    }

    return toConfig;
  }).reduce((prevConfig, toConfig) => toConfig(formValues, prevConfig), oldConfig.toBuilder());

  return newConfig.build();
};

const _onSubmit = (formValues: WidgetConfigFormValues, onConfigChange: (newConfig: AggregationWidgetConfig) => void, oldConfig: AggregationWidgetConfig) => {
  const newConfig = updateWidgetAggregationElements(formValues, oldConfig);

  return onConfigChange(newConfig);
};

const validateForm = (formValues: WidgetConfigFormValues) => {
  const elementValidations = aggregationElements.map((element) => element.validate ?? (() => ({}) as WidgetConfigValidationErrors));

  const elementValidationResults = elementValidations.map((validate) => validate(formValues));

  return elementValidationResults.reduce((prev, cur) => ({ ...prev, ...cur }), {});
};

const AggregationWizard = ({ onChange, config, children, onSubmit, onCancel }: EditWidgetComponentProps<AggregationWidgetConfig> & { children: React.ReactElement }) => {
  const initialFormValues = _initialFormValues(config);

  return (
    <WidgetConfigForm onSubmit={(formValues: WidgetConfigFormValues) => _onSubmit(formValues, onChange, config)}
                      initialValues={initialFormValues}
                      config={config}
                      validate={validateForm}>
      <>
        <Controls>
          <Section data-testid="configure-elements-section">
            <ElementsConfiguration config={config}
                                   onCreate={onCreateElement}
                                   onSubmit={onSubmit}
                                   onCancel={onCancel}
                                   onConfigChange={onChange} />
          </Section>
        </Controls>
        <VisualizationContainer>
          {children}
        </VisualizationContainer>
      </>
    </WidgetConfigForm>
  );
};

export default AggregationWizard;
