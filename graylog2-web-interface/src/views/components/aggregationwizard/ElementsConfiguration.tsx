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
import AggregationWidgetConfig from 'src/views/logic/aggregationbuilder/AggregationWidgetConfig';

import ElementConfigurationContainer from './elementConfiguration/ElementConfigurationContainer';
import type { AggregationElement } from './AggregationElements';
import type { WidgetConfigFormValues } from './WidgetConfigForm';

const _sortConfiguredElements = (
  values: WidgetConfigFormValues,
  aggregationElementsByKey: { [elementKey: string]: AggregationElement },
) => Object.keys(values).sort(
  (elementKey1, elementKey2) => (
    aggregationElementsByKey[elementKey1].order - aggregationElementsByKey[elementKey2].order
  ),
);
type Props = {
  aggregationElementsByKey: { [elementKey: string]: AggregationElement }
  config: AggregationWidgetConfig,
  onConfigChange: (config: AggregationWidgetConfig) => void,
}

const ElementsConfiguration = ({ aggregationElementsByKey, config, onConfigChange }: Props) => {
  const { values, setValues } = useFormikContext<WidgetConfigFormValues>();

  const _onDeleteElement = (aggregationElement) => {
    if (typeof aggregationElement.onDeleteAll !== 'function') {
      return;
    }

    setValues(aggregationElement.onDeleteAll(values));
  };

  return (
    <div>
      {_sortConfiguredElements(values, aggregationElementsByKey).map((elementKey) => {
        const aggregationElement = aggregationElementsByKey[elementKey];

        if (!aggregationElement) {
          throw new Error(`Aggregation element with key ${elementKey} is missing but configured for this widget.`);
        }

        const AggregationElementComponent = aggregationElement.component;

        return (
          <ElementConfigurationContainer isPermanentElement={aggregationElement.onDeleteAll === undefined}
                                         title={aggregationElement.title}
                                         onDeleteAll={() => _onDeleteElement(aggregationElement)}
                                         key={aggregationElement.key}>
            <AggregationElementComponent config={config} onConfigChange={onConfigChange} />
          </ElementConfigurationContainer>
        );
      })}
    </div>
  );
};

export default ElementsConfiguration;
