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

import type { GroupByFormValues, MetricFormValues, WidgetConfigFormValues } from '../WidgetConfigForm';

type Props = {
  index: number,
}

const directionOptions = [
  { label: 'Ascending', value: 'Ascending' },
  { label: 'Descending', value: 'Descending' },
];

const formatSeries = (metric: MetricFormValues) => {
  const readableField = `${metric.function}(${metric.field ?? ''})`;

  return {
    label: metric.name || readableField,
    field: readableField,
  };
};

const formatGrouping = (grouping: GroupByFormValues) => grouping.fields;

type OptionValue = {
  type: 'metric' | 'groupBy',
  field: string,
  label: string
};

type Option = {
  label: string,
  value: number,
};

const Sort = React.memo(({ index }: Props) => {
  const { values, setFieldValue } = useFormikContext<WidgetConfigFormValues>();
  const { metrics = [], groupBy: { groupings = [] } = {} } = values;
  const otherSorts = values?.sort?.filter((_, idx) => index !== idx) ?? [];
  const hasMetrics = otherSorts.find((s) => s.type === 'metric');
  const hasGroupings = otherSorts.find((s) => s.type === 'groupBy');

  const metricsOptions: Array<OptionValue> = hasGroupings ? [] : metrics
    .filter((metric) => metric.function !== 'latest')
    .map(formatSeries)
    .map(({ field, label }) => ({
      type: 'metric',
      field,
      label,
    }));
  const rowPivotOptions: Array<OptionValue> = (hasGroupings || hasMetrics) ? [] : groupings
    .filter((grouping) => (grouping.direction === 'row'))
    .flatMap(formatGrouping).map((groupBy) => ({
      type: 'groupBy',
      field: groupBy,
      label: groupBy,
    }));
  const options = [
    ...metricsOptions,
    ...rowPivotOptions,
  ];

  const numberIndexedOptions: Array<Option> = options.map((option, idx) => ({ label: option.label, value: idx }));

  const currentSort = values.sort[index];
  const optionIndex = options.findIndex((option) => (option.type === currentSort.type && option.field === currentSort.field));
  const selectedOption = optionIndex > -1 ? optionIndex : undefined;
  const invalidSort = selectedOption === undefined && 'field' in currentSort;

  return (
    <div data-testid={`sort-element-${index}`}>
      <Field name={`sort.${index}.field`}>
        {({ field: { name, onChange }, meta: { error } }) => {
          return (
            <Input id="field-select"
                   label="Field"
                   error={error}
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9">
              <Select options={invalidSort ? [{ label: currentSort.field, value: 0 }] : numberIndexedOptions}
                      disabled={invalidSort}
                      allowCreate={invalidSort}
                      clearable={false}
                      name={name}
                      value={invalidSort ? 0 : selectedOption}
                      placeholder="Specify field/metric to be sorted on"
                      aria-label="Select field for sorting"
                      size="small"
                      menuPortalTarget={document.body}
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
                    disabled={invalidSort}
                    clearable={false}
                    name={name}
                    aria-label="Select direction for sorting"
                    value={value}
                    size="small"
                    menuPortalTarget={document.body}
                    onChange={(newValue) => {
                      onChange({ target: { name, value: newValue } });
                    }} />
          </Input>
        )}
      </Field>
    </div>
  );
});

export default Sort;
