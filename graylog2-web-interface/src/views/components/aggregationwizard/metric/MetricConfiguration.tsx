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
import { Field, useFormikContext, getIn } from 'formik';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import styled from 'styled-components';

import { defaultCompare } from 'logic/DefaultCompare';
import { Input } from 'components/bootstrap';
import Select from 'components/common/Select';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';
import { InputOptionalInfo as Opt, FormikInput } from 'components/common';
import { Properties } from 'views/logic/fieldtypes/FieldType';
import useAggregationFunctions from 'views/hooks/useAggregationFunctions';

import FieldSelect from '../FieldSelect';

type Props = {
  index: number,
}

const Wrapper = styled.div``;

const sortByLabel = ({ label: label1 }: { label: string }, { label: label2 }: { label: string }) => defaultCompare(label1, label2);

const percentileOptions = [25.0, 50.0, 75.0, 90.0, 95.0, 99.0].map((value) => ({ label: value, value }));
const percentageStrategyOptions = [
  { label: 'Document Count', value: 'COUNT' },
  { label: 'Field Sum', value: 'SUM' },
];
const percentageRequiredFieldOptions = [Properties.Numeric];

const Metric = ({ index }: Props) => {
  const metricFieldSelectRef = useRef(null);
  const { data: functions, isLoading } = useAggregationFunctions();
  const functionOptions = useMemo(() => (isLoading ? [] : Object.values(functions)
    .map(({ type, description }) => ({ label: description, value: type }))
    .sort(sortByLabel)), [functions, isLoading]);

  const { values: { metrics }, errors: { metrics: metricsError }, setFieldValue } = useFormikContext<WidgetConfigFormValues>();
  const currentFunction = metrics[index].function;

  const hasFieldOption = currentFunction !== 'percentage';
  const isFieldRequired = !['count', 'percentage'].includes(currentFunction);

  const isPercentile = currentFunction === 'percentile';
  const isPercentage = currentFunction === 'percentage';
  const requiresNumericField = !['card', 'count', 'latest'].includes(currentFunction);
  const requiredProperties = requiresNumericField
    ? [Properties.Numeric]
    : [];

  const [functionIsSettled, setFunctionIsSettled] = useState<boolean>(false);
  const onFunctionChange = useCallback((newValue) => {
    setFieldValue(`metrics.${index}.function`, newValue);
    setFunctionIsSettled(true);
  }, [setFieldValue, index]);

  useEffect(() => {
    const metricError = getIn(metricsError?.[index], 'field');

    if (metricError && functionIsSettled) {
      metricFieldSelectRef.current.focus();
    }
  }, [functionIsSettled, metricsError, index, metricFieldSelectRef]);

  return (
    <Wrapper data-testid={`metric-${index}`}>
      <Field name={`metrics.${index}.function`}>
        {({ field: { name, value }, meta: { error } }) => (
          <Input id="metric-function-select"
                 label="Function"
                 error={error}
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <Select options={functionOptions}
                    clearable={false}
                    name={name}
                    value={value}
                    aria-label="Select a function"
                    size="small"
                    menuPortalTarget={document.body}
                    onChange={onFunctionChange} />
          </Input>
        )}
      </Field>
      {hasFieldOption && (
      <Field name={`metrics.${index}.field`}>
        {({ field: { name, value, onChange }, meta: { error } }) => (
          <Input id="metric-field"
                 label="Field"
                 error={error}
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <FieldSelect id="metric-field-select"
                         selectRef={metricFieldSelectRef}
                         onChange={(fieldName) => onChange({ target: { name, value: fieldName } })}
                         clearable={!isFieldRequired}
                         properties={requiredProperties}
                         name={name}
                         value={value}
                         ariaLabel="Select a field" />
          </Input>
        )}
      </Field>
      )}
      {isPercentile && (
        <Field name={`metrics.${index}.percentile`}>
          {({ field: { name, value, onChange }, meta: { error } }) => (
            <Input id="metric-percentile-select"
                   label="Percentile"
                   error={error}
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9">
              <Select options={percentileOptions}
                      clearable={false}
                      name={name}
                      value={value}
                      aria-label="Select percentile"
                      size="small"
                      menuPortalTarget={document.body}
                      onChange={(newValue) => onChange({ target: { name, value: newValue } })} />
            </Input>
          )}
        </Field>
      )}
      {isPercentage && (
        <>
          <Field name={`metrics.${index}.strategy`}>
            {({ field: { name, value, onChange }, meta: { error } }) => (
              <Input id="metric-percentage-strategy-select"
                     label="Strategy"
                     error={error}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9">
                <Select options={percentageStrategyOptions}
                        clearable={false}
                        name={name}
                        value={value}
                        aria-label="Select strategy"
                        size="small"
                        menuPortalTarget={document.body}
                        onChange={(newValue) => onChange({ target: { name, value: newValue } })} />
              </Input>
            )}
          </Field>
          <Field name={`metrics.${index}.field`}>
            {({ field: { name, value, onChange }, meta: { error } }) => (
              <Input id="metric-field"
                     label="Field"
                     error={error}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9">
                <FieldSelect id="metric-field-select"
                             selectRef={metricFieldSelectRef}
                             onChange={(fieldName) => onChange({ target: { name, value: fieldName } })}
                             clearable={!isFieldRequired}
                             properties={percentageRequiredFieldOptions}
                             name={name}
                             value={value}
                             ariaLabel="Select a field" />
              </Input>
            )}
          </Field>
        </>
      )}
      <FormikInput id="name"
                   label={<>Name <Opt /></>}
                   bsSize="small"
                   placeholder="Specify display name"
                   name={`metrics.${index}.name`}
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9" />
    </Wrapper>
  );
};

export default Metric;
