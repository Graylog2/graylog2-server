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
import { useFormikContext } from 'formik';
import { isEmpty } from 'lodash';
import styled from 'styled-components';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import ElementConfigurationSection from './elementConfigurationSections/ElementConfigurationSection';
import ElementsConfigurationActions from './ElementsConfigurationActions';
import type { AggregationElement } from './aggregationElements/AggregationElementType';
import type { WidgetConfigFormValues } from './WidgetConfigForm';

const Container = styled.div`
  position: relative;
`;

const _sortConfiguredElements = (
  values: WidgetConfigFormValues,
  aggregationElementsByKey: { [elementKey: string]: AggregationElement },
) => Object.entries(values).sort(
  ([elementKey1], [elementKey2]) => (
    aggregationElementsByKey[elementKey1].order - aggregationElementsByKey[elementKey2].order
  ),
);

type Props = {
  aggregationElementsByKey: { [elementKey: string]: AggregationElement }
  config: AggregationWidgetConfig,
  onConfigChange: (config: AggregationWidgetConfig) => void,
  onAddEmptyElement: (
    elementKey: string,
    values: WidgetConfigFormValues,
    setValues: (formValues: WidgetConfigFormValues) => void,
  ) => void,
}

const ElementsConfiguration = ({ aggregationElementsByKey, config, onConfigChange, onAddEmptyElement }: Props) => {
  const { values, setValues, dirty } = useFormikContext<WidgetConfigFormValues>();

  return (
    <Container>
      <div>
        {_sortConfiguredElements(values, aggregationElementsByKey).map(([elementKey, elementFormValues]) => {
          if (isEmpty(elementFormValues)) {
            return null;
          }

          const aggregationElement = aggregationElementsByKey[elementKey];

          if (!aggregationElement) {
            throw new Error(`Aggregation element with key ${elementKey} is missing but configured for this widget.`);
          }

          const ConfigurationSection = aggregationElement.component;

          return (
            <ElementConfigurationSection allowAddEmptyElement={aggregationElement.allowAddEmptyElement(values)}
                                         onAddEmptyElement={() => onAddEmptyElement(aggregationElement.key, values, setValues)}
                                         elementTitle={aggregationElement.title}
                                         sectionTitle={aggregationElement.sectionTitle}
                                         key={aggregationElement.key}>
              <ConfigurationSection config={config} onConfigChange={onConfigChange} />
            </ElementConfigurationSection>
          );
        })}
      </div>
      {dirty && <ElementsConfigurationActions />}
    </Container>
  );
};

export default ElementsConfiguration;
