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
import { EditWidgetComponentProps } from 'views/types';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import WidgetConfigForm, { WidgetConfigFormValues } from './WidgetConfigForm';
import AggregationElementSelect from './AggregationElementSelect';
import ElementsConfiguration from './ElementsConfiguration';
import aggregationElements from './aggregationElements';

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
  min-width: 315px;
  max-width: 500px;
  flex: 1.2;
  padding-right: 15px;
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

const onCreate = (
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

const _onSubmit = (formValues: WidgetConfigFormValues, onConfigChange: (newConfig: AggregationWidgetConfig) => void) => {
  const toConfigByKey = Object.fromEntries(aggregationElements.map(({ key, toConfig }) => [key, toConfig]));

  const newConfig = Object.keys(formValues).map((key) => {
    const toConfig = toConfigByKey[key] ?? ((_values, prevConfig) => prevConfig);

    if (!toConfig) {
      throw new Error(`Aggregation element with key ${key} is missing toConfig.`);
    }

    return toConfig;
  }).reduce((prevConfig, toConfig) => toConfig(formValues, prevConfig), AggregationWidgetConfig.builder());

  onConfigChange(newConfig.build());
};

const validateForm = (formValues: WidgetConfigFormValues) => {
  const elementValidations = aggregationElements.map((element) => element.validate ?? (() => ({})));

  const elementValidationResults = elementValidations.map((validate) => validate(formValues));

  return elementValidationResults.reduce((prev, cur) => ({ ...prev, ...cur }), {});
};

const AggregationWizard = ({ onChange, config, children }: EditWidgetComponentProps<AggregationWidgetConfig>) => {
  const initialFormValues = _initialFormValues(config);

  return (
    <Wrapper>
      <Controls>
        <WidgetConfigForm onSubmit={(formValues: WidgetConfigFormValues) => _onSubmit(formValues, onChange)}
                          initialValues={initialFormValues}
                          validate={validateForm}>
          {({ values, setValues }) => (
            <>
              <Section data-testid="add-element-section">
                <AggregationElementSelect onCreate={(elementKey) => onCreate(elementKey, values, setValues)}
                                          aggregationElements={aggregationElements}
                                          formValues={values} />
              </Section>
              <Section data-testid="configure-elements-section">
                <ElementsConfiguration aggregationElementsByKey={aggregationElementsByKey}
                                       config={config}
                                       onCreate={onCreate}
                                       onConfigChange={onChange} />
              </Section>
            </>
          )}
        </WidgetConfigForm>
      </Controls>
      <Visualization>
        {children}
      </Visualization>
    </Wrapper>
  );
};

export default AggregationWizard;
