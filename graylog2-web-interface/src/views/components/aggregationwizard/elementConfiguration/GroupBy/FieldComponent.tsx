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

import FieldSelect from 'views/components/aggregationwizard/elementConfiguration/FieldSelect';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import { WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';

type Props = {
  index: number,
  fieldType: string,
};

const FieldComponent = ({ index, fieldType }: Props) => {
  const fieldTypes = useContext(FieldTypesContext);
  const { setFieldValue } = useFormikContext<WidgetConfigFormValues>();

  const onChangeField = (e, name, onChange) => {
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
        setFieldValue(`groupBy.groupings.${index}.limit`, 15);
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
