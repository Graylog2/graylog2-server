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

import Select from 'views/components/Select';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import { defaultCompare } from 'views/logic/DefaultCompare';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';

import { PivotList, SeriesList, SortList } from './AggregationBuilderPropTypes';

const mapFields = (fields) => fields.sort(defaultCompare)
  .map((v, idx) => ({ label: v.label, value: idx }));

const findIdxInFields = (fields: SortOption[], sort) => Immutable.List<SortOption>(fields)
  .map((field) => field.value)
  .toList()
  .findIndex((field) => sort && (field.type === sort.type && field.field === sort.field));

const mapNewValue = (fields, idx) => (fields[idx] ? [fields[idx].value] : []);

type Props = {
  pivots: Array<Pivot>,
  series: Array<Series>,
  onChange: (newSorts: Array<any>) => any,
  sort: Array<SortConfig>,
};

const currentValue = (sort, fields: SortOption[]) => {
  const currentIdx = sort && findIdxInFields(fields, sort[0]);

  if (currentIdx === undefined) {
    return undefined;
  }

  return fields[currentIdx];
};

type SortOption = {
  label: string;
  value: SortConfig;
};

const SortSelect = ({ pivots, series, onChange, sort }: Props) => {
  const pivotOptions = pivots.map((pivot) => ({ label: pivot.field, value: SortConfig.fromPivot(pivot) }));
  const seriesOptions = series.map((s) => ({ label: s.effectiveName, value: SortConfig.fromSeries(s) }));
  const fields: SortOption[] = [].concat(pivotOptions, seriesOptions);
  const options = mapFields(fields);

  const _onChange = (newValue, reason) => {
    if (reason.action === 'clear') {
      return onChange([]);
    }

    const { value } = newValue;
    const mappedValue = mapNewValue(fields, value);

    return onChange(mappedValue);
  };

  return (
    <Select placeholder="None: click to add fields"
            onChange={_onChange}
            options={options}
            isClearable
            value={currentValue(sort, fields) ?? null} />
  );
};

SortSelect.propTypes = {
  pivots: PivotList.isRequired,
  series: SeriesList.isRequired,
  onChange: PropTypes.func.isRequired,
  sort: SortList.isRequired,
};

export default SortSelect;
