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

import { DropdownButton, MenuItem } from 'components/graylog';

import type { AggregationElement } from './aggregationElements/AggregationElementType';
import type { WidgetConfigFormValues } from './WidgetConfigForm';

type Props = {
  aggregationElements: Array<AggregationElement>,
  formValues: WidgetConfigFormValues,
  onSelect: (elementKey: string) => void,
}

const AggregationElementSelect = ({ aggregationElements, onSelect, formValues }: Props) => {
  const menuItems = aggregationElements
    .filter(({ allowCreate }) => allowCreate(formValues))
    .map(({ key, title }) => <MenuItem key={`element-select-${key}`} onSelect={() => onSelect(key)}>{title}</MenuItem>);

  return (
    <DropdownButton id="add-aggregation-element" dropup title="Add">
      {menuItems}
    </DropdownButton>
  );
};

export default AggregationElementSelect;
