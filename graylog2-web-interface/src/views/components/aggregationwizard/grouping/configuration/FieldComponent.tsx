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
import { useCallback, useContext } from 'react';
import { useFormikContext } from 'formik';

import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldsConfiguration from 'views/components/widgets/FieldsConfiguration';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import type { GroupByFormValues, WidgetConfigFormValues } from 'views/components/aggregationwizard/WidgetConfigForm';
import Input from 'components/bootstrap/Input';
import type { GroupByError } from 'views/components/aggregationwizard/grouping/GroupingElement';
import { onGroupingFieldsChange } from 'views/components/aggregationwizard/grouping/GroupingElement';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import { DateType, ValuesType } from 'views/logic/aggregationbuilder/Pivot';
import useFeature from 'hooks/useFeature';
import { UNIT_FEATURE_FLAG } from 'views/components/visualizations/Constants';

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
  const unitFeatureEnabled = useFeature(UNIT_FEATURE_FLAG);
  const showFieldUnit = unitFeatureEnabled && values?.visualization?.type === 'table';
  const grouping = values.groupBy.groupings[groupingIndex];
  const activeQueryId = useActiveQueryId();
  const createSelectPlaceholder = placeholder(grouping);

  const onChangeSelectedFields = (newFields: Array<string>) => {
    onGroupingFieldsChange({
      fieldTypes,
      activeQueryId,
      groupingIndex,
      grouping,
      newFields,
      setFieldValue,
    });
  };

  const isFieldQualified = useCallback((field: FieldTypeMapping) => {
    if (!grouping.fields?.length) {
      return true;
    }

    const fieldTypeCategory = field.type.type === 'date' ? DateType : ValuesType;

    return grouping.type === fieldTypeCategory;
  }, [grouping.fields?.length, grouping.type]);

  return (
    <Input id="group-by-field-select"
           label="Fields"
           labelClassName="col-sm-3"
           error={(errors?.groupBy?.groupings?.[groupingIndex] as GroupByError)?.fields}
           wrapperClassName="col-sm-9">
      <FieldsConfiguration onChange={onChangeSelectedFields}
                           selectedFields={grouping.fields}
                           menuPortalTarget={document.body}
                           createSelectPlaceholder={createSelectPlaceholder}
                           isFieldQualified={isFieldQualified}
                           testPrefix={`grouping-${groupingIndex}`}
                           showUnit={showFieldUnit} />
    </Input>
  );
};

export default FieldComponent;
