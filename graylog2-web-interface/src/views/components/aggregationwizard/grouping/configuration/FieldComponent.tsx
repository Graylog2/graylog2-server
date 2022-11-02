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
import { useContext } from 'react';

import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';
import parseNumber from 'views/components/aggregationwizard/grouping/parseNumber';

import FieldSelect from '../../FieldSelect';

type Props = {
  index: number,
  fieldType: string,
};

const numberNotSet = (value: string | number | undefined) => parseNumber(value) === undefined;

const defaultLimit = 15;

const FieldComponent = ({ index, fieldType }: Props) => {
  const fieldTypes = useContext(FieldTypesContext);
  const { setFieldValue, values } = useFormikContext<WidgetConfigFormValues>();
  const grouping = values.groupBy.groupings[index];

  const onChangeField = (e: { target: { name: string, value: string } }, name: string, onChange) => {
    const fieldName = e.target.value;
    const newField = fieldTypes.all.find((field) => field.name === fieldName);
    const newFieldType = newField?.type.type === 'date' ? 'time' : 'values';

    if (fieldType !== newFieldType) {
      if (newFieldType === 'time') {
        setFieldValue(`groupBy.groupings.${index}.interval`, {
          type: 'auto',
          scaling: 1.0,
        });
      }

      if (newFieldType === 'values') {
        setFieldValue(`groupBy.groupings.${index}.interval`, undefined, false);

        if (grouping.direction === 'row' && numberNotSet(values.groupBy.rowLimit)) {
          setFieldValue('groupBy.rowLimit', defaultLimit);
        }

        if (grouping.direction === 'column' && numberNotSet(values.groupBy.columnLimit)) {
          setFieldValue('groupBy.columnLimit', defaultLimit);
        }
      }
    }

    onChange({
      target: {
        name,
        value: {
          field: newField.name,
          type: newFieldType,
        },
      },
    });
  };

  return (
    <Field name={`groupBy.groupings.${index}.field`}>
      {({ field: { name, value, onChange }, meta: { error } }) => (
        <FieldSelect id="group-by-field-select"
                     label="Field"
                     onChange={(e) => onChangeField(e, name, onChange)}
                     error={error}
                     clearable={false}
                     ariaLabel="Field"
                     name={name}
                     value={value.field}
                     aria-label="Select a field" />
      )}
    </Field>
  );
};

export default FieldComponent;
