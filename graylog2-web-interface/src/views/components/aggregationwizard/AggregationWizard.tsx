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

import { ButtonToolbar } from 'components/graylog';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Button from 'components/graylog/Button';

import WidgetConfigForm, { WidgetConfigFormValues } from './WidgetConfigForm';
import AggregationElementSelect from './AggregationElementSelect';
import ElementsConfiguration from './ElementsConfiguration';
import aggregationElements from './AggregationElements';

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

const _onElementCreate = (
  elementKey: string,
  values: WidgetConfigFormValues,
  setValues: (formValues: WidgetConfigFormValues) => void,
) => {
  setValues({
    ...values,
    [elementKey]: [
      ...(values[elementKey] ?? []),
      {},
    ],
  });
};

const _onSubmit = (formValues: WidgetConfigFormValues, onConfigChange: (newConfig: AggregationWidgetConfig) => void) => {
  const toConfigByKey = Object.fromEntries(aggregationElements.map(({ key, toConfig }) => [key, toConfig]));

  const newConfig = Object.keys(formValues).map((key) => {
    const toConfig = toConfigByKey[key] ?? ((_values, prevConfig) => prevConfig);

    if (!toConfig) {
      throw new Error(`Aggregation element with key ${key} is missing toConfig.`);
    }

    return toConfig;
  }).reduce((prevConfig, toConfig) => toConfig(formValues, prevConfig), AggregationWidgetConfig.builder().build());

  onConfigChange(newConfig);
};

const validateForm = (formValues: WidgetConfigFormValues) => {
  const elementValidations = aggregationElements.map((element) => element.validate ?? (() => ({})));

  const elementValidationResults = elementValidations.map((validate) => validate(formValues));

  return elementValidationResults.reduce((prev, cur) => ({ ...prev, ...cur }), {});
};

const StyledButtonToolbar = styled(ButtonToolbar)`
  margin-top: 5px;
`;

const AggregationWizard = ({ onChange, config, children }: EditWidgetComponentProps<AggregationWidgetConfig>) => {
  const initialFormValues = _initialFormValues(config);

  return (
    <Wrapper>
      <Controls>
        <WidgetConfigForm onSubmit={(formValues: WidgetConfigFormValues) => _onSubmit(formValues, onChange)}
                          initialValues={initialFormValues}
                          validate={validateForm}>
          {({ isValid, dirty, values, setValues }) => (
            <>
              <Section data-testid="add-element-section">
                <SectionHeadline>Add an Element</SectionHeadline>
                <AggregationElementSelect onElementCreate={(elementKey) => _onElementCreate(elementKey, values, setValues)}
                                          aggregationElements={aggregationElements} />
              </Section>
              <Section data-testid="configure-elements-section">
                <SectionHeadline>Configured Elements</SectionHeadline>
                <ElementsConfiguration aggregationElementsByKey={aggregationElementsByKey}
                                       config={config}
                                       onConfigChange={onChange} />
                {dirty && (
                  <StyledButtonToolbar>
                    <Button bsStyle="primary" className="pull-right" type="submit" disabled={!isValid}>Apply Changes</Button>
                  </StyledButtonToolbar>
                )}
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
