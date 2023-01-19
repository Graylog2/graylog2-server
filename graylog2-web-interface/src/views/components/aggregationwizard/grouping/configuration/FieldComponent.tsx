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
import { useContext, useCallback } from 'react';
import { useFormikContext } from 'formik';

import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import type { WidgetConfigFormValues, GroupByFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';
import Input from 'components/bootstrap/Input';
import SelectedFieldsList from 'views/components/aggregationwizard/grouping/configuration/SelectedFieldsList';
import type { GroupByError } from 'views/components/aggregationwizard/grouping/GroupingElement';
import { onGroupingFieldsChange } from 'views/components/aggregationwizard/grouping/GroupingElement';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import { DateType } from 'views/logic/aggregationbuilder/Pivot';

import FieldSelect from '../../FieldSelect';

const placeholder = (grouping: GroupByFormValues) => {
  if (!grouping.fields?.length) {
    return 'Add a field';
  }

  if (grouping.type === DateType) {
    return 'Add another date field';
  }

  return 'Add another field';
};

type Props = {
  groupingIndex: number,
};

const FieldComponent = ({ groupingIndex }: Props) => {
  const fieldTypes = useContext(FieldTypesContext);
  const { setFieldValue, values, errors } = useFormikContext<WidgetConfigFormValues>();
  const grouping = values.groupBy.groupings[groupingIndex];
  const activeQueryId = useActiveQueryId();

  const onAddField = useCallback((fieldName: string) => {
    const newFields = [...(grouping.fields ?? []), fieldName];

    onGroupingFieldsChange({
      fieldTypes,
      activeQueryId,
      groupingIndex,
      grouping,
      newFields,
      setFieldValue,
    });
  }, [activeQueryId, fieldTypes, grouping, groupingIndex, setFieldValue]);

  return (
    <Input id="group-by-field-select"
           label="Fields"
           labelClassName="col-sm-3"
           error={(errors?.groupBy?.groupings?.[groupingIndex] as GroupByError)?.fields}
           wrapperClassName="col-sm-9">
      <FieldSelect id="group-by-field-create-select"
                   onChange={onAddField}
                   clearable={false}
                   ariaLabel="Fields"
                   qualifiedTypeCategory={grouping.fields?.length ? grouping.type : undefined}
                   persistSelection={false}
                   name="group-by-field-create-select"
                   value={undefined}
                   excludedFields={grouping.fields ?? []}
                   placeholder={placeholder(grouping as GroupByFormValues)}
                   aria-label="Add a field" />
      <SelectedFieldsList groupingIndex={groupingIndex} />
    </Input>
  );
};

export default FieldComponent;
