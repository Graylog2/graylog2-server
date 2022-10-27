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
import styled from 'styled-components';

import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import StickyBottomActions from 'views/components/widgets/StickyBottomActions';
import SaveOrCancelButtons from 'views/components/widgets/SaveOrCancelButtons';

import aggregationElements from './aggregationElementDefinitions';
import ElementConfigurationSection from './ElementConfigurationSection';
import ElementsConfigurationActions from './ElementsConfigurationActions';
import type { WidgetConfigFormValues } from './WidgetConfigForm';

const Container = styled.div`
  position: relative;
  height: 100%;
`;

const aggregationElementsByKey = Object.fromEntries(aggregationElements.map((element) => ([element.key, element])));
const sortedConfiguredElements = Object.entries(aggregationElementsByKey).sort(
  ([elementKey1], [elementKey2]) => (
    aggregationElementsByKey[elementKey1].order - aggregationElementsByKey[elementKey2].order
  ),
);

type Props = {
  config: AggregationWidgetConfig,
  onConfigChange: (config: AggregationWidgetConfig) => void,
  onCreate: (
    elementKey: string,
    values: WidgetConfigFormValues,
    setValues: (formValues: WidgetConfigFormValues) => void,
  ) => void,
  onSubmit: () => void,
  onCancel: () => void,
}

const BottomActions = (onCancel, onSubmit) => (
  <>
    <ElementsConfigurationActions />
    <SaveOrCancelButtons onCancel={onCancel} onSubmit={onSubmit} />
  </>
);

const ElementsConfiguration = ({ config, onConfigChange, onCreate, onSubmit, onCancel }: Props) => {
  const { values } = useFormikContext<WidgetConfigFormValues>();

  return (
    <Container>
      <StickyBottomActions actions={<BottomActions onCancel={onCancel} onSubmit={onSubmit} />}>
        <div>
          {sortedConfiguredElements.map(([elementKey, aggregationElement]) => {
            if (!aggregationElement) {
              throw new Error(`Aggregation element with key ${elementKey} is missing but configured for this widget.`);
            }

            const ConfigurationSection = aggregationElement.component;

            return (
              <ElementConfigurationSection allowCreate={aggregationElement.allowCreate(values)}
                                           onCreate={onCreate}
                                           elementTitle={aggregationElement.title}
                                           sectionTitle={aggregationElement.sectionTitle}
                                           elementKey={aggregationElement.key}
                                           key={aggregationElement.key}>
                <ConfigurationSection config={config} onConfigChange={onConfigChange} />
              </ElementConfigurationSection>
            );
          })}
        </div>
      </StickyBottomActions>
    </Container>
  );
};

export default ElementsConfiguration;
