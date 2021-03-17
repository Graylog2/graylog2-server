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
import { Field } from 'formik';

import { defaultCompare } from 'views/logic/DefaultCompare';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import { Input } from 'components/bootstrap';
import Select from 'components/common/Select';

import { useStore } from 'stores/connect';
import AggregationFunctionsStore from 'views/stores/AggregationFunctionsStore';

type Props = {
  index: number,
}

const sortByLabel = ({ label: label1 }: { label: string }, { label: label2 }: { label: string }) => defaultCompare(label1, label2);

const Metric = ({ index }: Props) => {
  const functions = useStore(AggregationFunctionsStore);
  const fieldTypes = useContext(FieldTypesContext);
  const fieldTypeOptions = fieldTypes.all.map((fieldType) => ({ label: fieldType.name, value: fieldType.name })).toArray().sort(sortByLabel);
  const functionOptions = Object.values(functions).map(({ type }) => ({ label: type, value: type })).sort(sortByLabel);

  return (
    <>
      <Field name={`metrics.${index}.function`}>
        {({ field: { name, value, onChange } }) => (
          <Input id="function-select"
                 label="Function"
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <Select options={functionOptions}
                    name="function"
                    value={value}
                    onChange={(newValue) => {
                      onChange({ target: { name, value: newValue } });
                    }} />
        </Input>
      )}
      </Field>

      <Field name={`metrics.${index}.field`}>
        {({ field: { name, value, onChange } }) => (
          <Input id="field-select"
                 label="Field"
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9">
            <Select options={fieldTypeOptions}
                    name="field"
                    value={value}
                    onChange={(newValue) => onChange({ target: { name, value: newValue } })} />
          </Input>
        )}
      </Field>
    </>
  );
};

export default Metric;
