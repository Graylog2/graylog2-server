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
import type { WidgetConfigFormValues, GroupByFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';
import Input from 'components/bootstrap/Input';
import SelectedFieldsList from 'views/components/aggregationwizard/grouping/configuration/SelectedFieldsList';
import { DEFAULT_LIMIT, DEFAULT_PIVOT_INTERVAL } from 'views/Constants';

import FieldSelect from '../../FieldSelect';

type Props = {
  groupingIndex: number,
};

const FieldComponent = ({ groupingIndex }: Props) => {
  const fieldTypes = useContext(FieldTypesContext);
  const { setFieldValue, values } = useFormikContext<WidgetConfigFormValues>();
  const grouping = values.groupBy.groupings[groupingIndex];

  const onAddField = (fieldName: string) => {
    const field = fieldTypes.all.find(({ name }) => name === fieldName);
    const fieldType = field?.type.type === 'date' ? 'time' : 'values';
    const updateGrouping = (newGrouping: Partial<GroupByFormValues>) => setFieldValue(`groupBy.groupings.${groupingIndex}`, { ...grouping, ...newGrouping });

    if (!grouping.fields?.length) {
      if (fieldType === 'time') {
        updateGrouping({
          type: fieldType,
          fields: [fieldName],
          interval: DEFAULT_PIVOT_INTERVAL,
        });
      }

      if (fieldType === 'values') {
        updateGrouping({
          type: fieldType,
          fields: [fieldName],
          limit: DEFAULT_LIMIT,
        });
      }

      return;
    }

    updateGrouping({
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
