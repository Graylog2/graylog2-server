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
import { useContext } from 'react';
import { Field, useFormikContext } from 'formik';

import { defaultCompare } from 'views/logic/DefaultCompare';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import { Input } from 'components/bootstrap';
import Select from 'components/common/Select';
import { useStore } from 'stores/connect';
import AggregationFunctionsStore from 'views/stores/AggregationFunctionsStore';
import { WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';
import FormikInput from 'components/common/FormikInput';

type Props = {
  index: number,
}

const sortByLabel = ({ label: label1 }: { label: string }, { label: label2 }: { label: string }) => defaultCompare(label1, label2);

const percentileOptions = [25.0, 50.0, 75.0, 90.0, 95.0, 99.0].map((value) => ({ label: value, value }));

const Metric = ({ index }: Props) => {
  const functions = useStore(AggregationFunctionsStore);
  const fieldTypes = useContext(FieldTypesContext);
  const fieldTypeOptions = fieldTypes.all.map((fieldType) => ({ label: fieldType.name, value: fieldType.name })).toArray().sort(sortByLabel);
  const functionOptions = Object.values(functions).map(({ type }) => ({ label: type, value: type })).sort(sortByLabel);

  const { values } = useFormikContext<WidgetConfigFormValues>();
  const currentFunction = values.metrics[index].function;

  const isFieldRequired = currentFunction !== 'count';

  const isPercentile = currentFunction === 'percentile';

  return (
    <>
      <FormikInput id="name"
                   label="Name"
                   placeholder="Specify optional name"
                   name={`metrics.${index}.name`}
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9" />

      <Field name={`metrics.${index}.function`}>
        {({ field: { name, value, onChange }, meta: { error } }) => (
          <Input id="function-select"
                 label="Function"
                 error={error}
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <Select options={functionOptions}
                    clearable={false}
                    name={name}
                    value={value}
                    aria-label="Select a function"
                    onChange={(newValue) => {
                      onChange({ target: { name, value: newValue } });
                    }} />
          </Input>
        )}
      </Field>

      <Field name={`metrics.${index}.field`}>
        {({ field: { name, value, onChange }, meta: { error } }) => (
          <Input id="field-select"
                 label="Field"
                 error={error}
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <Select options={fieldTypeOptions}
                    clearable={!isFieldRequired}
                    name={name}
                    value={value}
                    aria-label="Select a field"
                    onChange={(newValue) => onChange({ target: { name, value: newValue } })} />
          </Input>
        )}
      </Field>
      {isPercentile && (
        <Field name={`metrics.${index}.percentile`}>
          {({ field: { name, value, onChange }, meta: { error } }) => (
            <Input id="percentile-select"
                   label="Percentile"
                   error={error}
                   labelClassName="col-sm-3"
                   wrapperClassName="col-sm-9">
              <Select options={percentileOptions}
                      clearable={false}
                      name={name}
                      value={value}
                      aria-label="Select percentile"
                      onChange={(newValue) => onChange({ target: { name, value: newValue } })} />
            </Input>
          )}
        </Field>
      )}
    </>
  );
};

export default Metric;
