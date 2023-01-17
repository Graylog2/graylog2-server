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
import { useFormikContext } from 'formik';
import { useContext } from 'react';

import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';
import Input from 'components/bootstrap/Input';
import SelectedFieldsList from 'views/components/aggregationwizard/grouping/configuration/SelectedFieldsList';

import FieldSelect from '../../FieldSelect';

type Props = {
  groupingIndex: number,
};

const FieldComponent = ({ groupingIndex }: Props) => {
  const fieldTypes = useContext(FieldTypesContext);
  const { setFieldValue, values } = useFormikContext<WidgetConfigFormValues>();
  const grouping = values.groupBy.groupings[groupingIndex];

  const onAddField = (e: { target: { name: string, value: string } }) => {
    const fieldName = e.target.value;
    const newField = fieldTypes.all.find((field) => field.name === fieldName);
    const newFieldType = newField?.type.type === 'date' ? 'time' : 'values';

    setFieldValue(`groupBy.groupings.${groupingIndex}`, {
      ...grouping,
      type: newFieldType,
      fields: [...(grouping.fields ?? []), fieldName],
    });
  };

  return (
    <Input id="group-by-field-select"
           label="Fields"
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9">
      <FieldSelect id="group-by-field-create-select"
                   onChange={(e) => onAddField(e)}
                   clearable={false}
                   ariaLabel="Fields"
                   persistSelection={false}
                   name="group-by-field-create-select"
                   value={undefined}
                   excludedFields={grouping.fields ?? []}
                   placeholder="Add a field"
                   aria-label="Add a field" />
      <SelectedFieldsList groupingIndex={groupingIndex} />
    </Input>
  );
};

export default FieldComponent;
