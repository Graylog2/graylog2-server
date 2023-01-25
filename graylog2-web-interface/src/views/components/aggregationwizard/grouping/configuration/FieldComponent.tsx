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
import { DEFAULT_LIMIT } from 'views/Constants';

import FieldSelect from '../../FieldSelect';

type Props = {
  index: number,
  fieldType: string,
};

const numberNotSet = (value: string | number | undefined) => parseNumber(value) === undefined;

const defaultLimit = DEFAULT_LIMIT;

const FieldComponent = ({ index, fieldType }: Props) => {
  const fieldTypes = useContext(FieldTypesContext);
  const { setFieldValue, values } = useFormikContext<WidgetConfigFormValues>();
  const grouping = values.groupBy.groupings[index];

  const onChangeField = (e: { target: { name: string, value: string } }) => {
    const fieldName = e.target.value;
    const newField = fieldTypes.all.find((field) => field.name === fieldName);
    const newFieldType = newField?.type.type === 'date' ? 'time' : 'values';

    if (fieldType !== newFieldType) {
      if (newFieldType === 'time') {
        setFieldValue(`groupBy.groupings.${index}`, {
          type: newFieldType,
          fields: [fieldName],
          interval: {
            type: 'auto',
            scaling: 1.0,
          },
        });
      }

      if (newFieldType === 'values') {
        setFieldValue(`groupBy.groupings.${index}`, {
          type: newFieldType,
          fields: [fieldName],
          limit: defaultLimit,
        });

        setFieldValue(`groupBy.groupings.${index}.interval`, undefined, false);

        if (!('limit' in grouping) || ('limit' in grouping && numberNotSet(grouping.limit))) {
          setFieldValue(`groupBy.groupings.${index}.limit`, defaultLimit);
        }
      }

      return;
    }

    setFieldValue(`groupBy.groupings.${index}`, {
      ...grouping,
      type: newFieldType,
      fields: [fieldName],
    });
  };

  return (
    <Field name={`groupBy.groupings.${index}.fields`}>
      {({ field: { name, value }, meta: { error } }) => (
        <FieldSelect id="group-by-field-select"
                     label="Field"
                     onChange={(e) => onChangeField(e)}
                     error={error}
                     clearable={false}
                     ariaLabel="Field"
                     name={name}
                     value={value?.[0]}
                     aria-label="Select a field" />
      )}
    </Field>
  );
};

export default FieldComponent;
