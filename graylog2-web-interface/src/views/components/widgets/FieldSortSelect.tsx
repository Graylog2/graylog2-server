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
import { useMemo } from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';

import CustomPropTypes from 'views/components/CustomPropTypes';
import { defaultCompare } from 'views/logic/DefaultCompare';
import Direction from 'views/logic/aggregationbuilder/Direction';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import Select from 'components/common/Select';

type Props = {
  fields: Immutable.List<FieldTypeMapping>,
  onChange: (newSort: Array<SortConfig>) => void,
  sort: Array<SortConfig>,
};

type Option = {
  label: string,
  value: number,
};

const findOptionByLabel = (options: Array<Option>, label: string) => options.find((option) => option.label === label);

const findOptionByValue = (options: Array<Option>, value: number) => options.find((option) => option.value === value);

const currentValue = (sort: Array<SortConfig>, options: Array<Option>) => sort && sort.length > 0 && findOptionByLabel(options, sort[0].field)?.value;

const sortedOptions = (fields: Immutable.List<FieldTypeMapping>): Array<Option> => {
  return fields.sort(
    (field1, field2) => defaultCompare(field1.name, field2.name),
  ).map(
    (field, idx) => ({ label: field.name, value: idx }),
  ).toArray();
};

const onOptionChange = (options: Array<Option>, onChange, value) => {
  const option = findOptionByValue(options, value);

  if (!option) {
    return undefined;
  }

  const sortConfig = new SortConfig(SortConfig.PIVOT_TYPE, option.label, Direction.Ascending);

  return onChange([sortConfig]);
};

const FieldSortSelect = ({ fields, onChange, sort }: Props) => {
  const options = useMemo(() => sortedOptions(fields), [fields]);

  return (
    <Select placeholder="None: click to add fields"
            onChange={(newValue) => onOptionChange(options, onChange, newValue)}
            options={options}
            clearable={false}
            aria-label="Select field for sorting"
            value={currentValue(sort, options)} />
  );
};

FieldSortSelect.propTypes = {
  onChange: PropTypes.func.isRequired,
  fields: CustomPropTypes.FieldListType.isRequired,
  sort: PropTypes.array.isRequired,
};

export default FieldSortSelect;
