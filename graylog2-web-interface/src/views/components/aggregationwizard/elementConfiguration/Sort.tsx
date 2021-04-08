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
import { Field, useFormikContext } from 'formik';

import Select from 'components/common/Select';
import { Input } from 'components/bootstrap';
import {
  GroupByFormValues,
  MetricFormValues,
  WidgetConfigFormValues,
} from 'views/components/aggregationwizard/WidgetConfigForm';

type Props = {
  index: number,
}

const directionOptions = [
  { label: 'Ascending', value: 'Ascending' },
  { label: 'Descending', value: 'Descending' },
];

const formatSeries = (metric: MetricFormValues) => (metric.name ? metric.name : `${metric.function}(${metric.field ?? ''})`);
const formatGrouping = (grouping: GroupByFormValues) => grouping.field.field;

type OptionValue = {
  type: 'metric' | 'groupBy',
  field: string,
};

type Option = {
  label: string,
  value: number,
};

const Sort = ({ index }: Props) => {
  const { values, setFieldValue } = useFormikContext<WidgetConfigFormValues>();
  const { metrics = [], groupBy: { groupings = [] } = {} } = values;

  const metricsOptions: Array<OptionValue> = metrics.map(formatSeries).map((metric) => ({ type: 'metric', field: metric }));
  const rowPivotOptions: Array<OptionValue> = groupings.filter((grouping) => (grouping.direction === 'row')).map(formatGrouping).map((groupBy) => ({ type: 'groupBy', field: groupBy }));
  const options = [
    ...metricsOptions,
    ...rowPivotOptions,
  ];

  const numberIndexedOptions: Array<Option> = options.map((option, idx) => ({ label: option.field, value: idx }));

  const currentSort = values.sort[index];
  const selectedOption = currentSort ? options.findIndex((option) => (option.type === currentSort.type && option.field === currentSort.field)) : undefined;

  return (
    <>
      <Field name={`sort.${index}.field`}>
        {({ field: { name, onChange }, meta: { error } }) => {
          return (
            <Input id="field-select"
                   label="Field"
                   error={error}
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9">
              <Select options={numberIndexedOptions}
                      clearable={false}
                      name={name}
                      value={selectedOption}
                      placeholder="Specify field/metric to be sorted on"
                      aria-label="Select field for sorting"
                      size="small"
                      onChange={(newValue: Option['value']) => {
                        const option = options[newValue];
                        setFieldValue(`sort.${index}.type`, option.type);
                        onChange({ target: { name, value: option.field } });
                      }} />
            </Input>
          );
        }}
      </Field>

      <Field name={`sort.${index}.direction`}>
        {({ field: { name, value, onChange }, meta: { error } }) => (
          <Input id="direction-select"
                 label="Direction"
                 error={error}
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <Select options={directionOptions}
                    clearable={false}
                    name={name}
                    value={value}
                    aria-label="Select direction for sorting"
                    size="small"
                    onChange={(newValue) => {
                      onChange({ target: { name, value: newValue } });
                    }} />
          </Input>
        )}
      </Field>
    </>
  );
};

export default Sort;
