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
import { useRef } from 'react';

import { Select } from 'components/common';

import type { AggregationElement } from './aggregationElements/AggregationElementType';
import type { WidgetConfigFormValues } from './WidgetConfigForm';

const _getOptions = (aggregationElements: Array<AggregationElement>, formValues: WidgetConfigFormValues) => {
  return aggregationElements.reduce((availableElements, aggregationElement) => {
    if (!aggregationElement.allowAddEmptyElement(formValues)) {
      return availableElements;
    }

    availableElements.push({ value: aggregationElement.key, label: aggregationElement.title });

    return availableElements;
  }, []);
};

type Props = {
  aggregationElements: Array<AggregationElement>,
  formValues: WidgetConfigFormValues,
  onSelect: (elementKey: string) => void,
}

const AggregationElementSelect = ({ aggregationElements, onSelect, formValues }: Props) => {
  const selectRef = useRef(null);
  const options = _getOptions(aggregationElements, formValues);

  const _onSelect = (elementKey: string) => {
    selectRef.current.clearValue();
    onSelect(elementKey);
  };

  return (
    <Select options={options}
            onChange={_onSelect}
            ref={selectRef}
            placeholder="Select an element to add ..."
            aria-label="Select an element to add ..." />
  );
};

export default AggregationElementSelect;
