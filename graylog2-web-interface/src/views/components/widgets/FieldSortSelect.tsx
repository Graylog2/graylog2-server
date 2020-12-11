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
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';

import CustomPropTypes from 'views/components/CustomPropTypes';
import { defaultCompare } from 'views/logic/DefaultCompare';
import Direction from 'views/logic/aggregationbuilder/Direction';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import Select from 'views/components/Select';

type Props = {
  fields: Immutable.List<FieldTypeMapping>,
  onChange: (newSort: Array<SortConfig>) => void,
  sort: Array<SortConfig>,
};

type Option = {
  label: string,
  value: number,
};

const findOptionByLabel = (options: Immutable.List<Option>, label: string) => options.find((option) => option.label === label);

const findOptionByValue = (options: Immutable.List<Option>, value: number) => options.find((option) => option.value === value);

const currentValue = (sort: Array<SortConfig>, options: Immutable.List<Option>) => sort && sort.length > 0 && findOptionByLabel(options, sort[0].field);

const sortedOptions = (fields: Immutable.List<FieldTypeMapping>) => {
  return fields.sort(
    (field1, field2) => defaultCompare(field1.name, field2.name),
  ).map(
    (field, idx) => ({ label: field.name, value: idx }),
  ).toList();
};

const onOptionChange = (options: Immutable.List<Option>, onChange, newValue, reason) => {
  if (reason.action === 'clear') {
    return onChange([]);
  }

  const { value } = newValue;
  const option = findOptionByValue(options, value);

  if (!option) {
    return undefined;
  }

  const sortConfig = new SortConfig(SortConfig.PIVOT_TYPE, option.label, Direction.Ascending);

  return onChange([sortConfig]);
};

const FieldSortSelect = ({ fields, onChange, sort }: Props) => {
  const options = sortedOptions(fields);

  return (
    <Select placeholder="None: click to add fields"
            onChange={(newValue, reason) => onOptionChange(options, onChange, newValue, reason)}
            options={options.toJS()}
            isClearable
            value={currentValue(sort, options)} />
  );
};

FieldSortSelect.propTypes = {
  onChange: PropTypes.func.isRequired,
  fields: CustomPropTypes.FieldListType.isRequired,
  sort: PropTypes.array.isRequired,
};

export default FieldSortSelect;
