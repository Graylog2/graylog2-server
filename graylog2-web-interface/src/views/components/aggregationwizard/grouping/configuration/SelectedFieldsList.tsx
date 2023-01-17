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
import styled from 'styled-components';
import { useState } from 'react';
import { useFormikContext } from 'formik';

// import { DEFAULT_LIMIT } from 'views/Constants';
// import parseNumber from 'views/components/aggregationwizard/grouping/parseNumber';
import { IconButton } from 'components/common';
import FieldSelect from 'views/components/aggregationwizard/FieldSelect';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';
// import FieldTypesContext from 'views/components/contexts/FieldTypesContext';

const ListItemContainer = styled.li`
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

const EditFieldSelect = styled(FieldSelect)`
  flex: 1;
`;

const Title = styled.div`
  display: flex;
  align-items: center;
`;

type ListItemProps = {
  excludedFields: Array<string>,
  fieldName: string,
  onChange: (fieldName: string) => void,
}

const ListItem = ({ fieldName, onChange, excludedFields }: ListItemProps) => {
  const [isEditing, setIsEditing] = useState(false);

  const _onChange = (fieldName: string) => {
    onChange(fieldName);
    setIsEditing(false);
  };

  return (
    <ListItemContainer>
      {isEditing ? (
        <EditFieldSelect id="group-by-field-select"
                         onChange={_onChange}
                         menuIsOpen
                         autoFocus
                         clearable={false}
                         excludedFields={excludedFields}
                         ariaLabel="Fields"
                         name="group-by-field-create-select"
                         value={fieldName}
                         placeholder="Add a field"
                         aria-label="Add a field" />
      ) : (
        <>
          <Title><IconButton name="bars" />{fieldName}</Title>
          <div>
            <IconButton name="edit" onClick={() => setIsEditing(true)} />
            <IconButton name="trash-alt" />
          </div>
        </>
      )}
    </ListItemContainer>
  );
};

const FieldsList = styled.ul`
  padding: 0;
`;

type Props = {
  groupingIndex: number,
};

const SelectedFieldsList = ({ groupingIndex }: Props) => {
  const { setFieldValue, values } = useFormikContext<WidgetConfigFormValues>();
  const grouping = values.groupBy.groupings[groupingIndex];

  if (!grouping.fields?.length) {
    return null;
  }

  const onChangeFieldName = (fieldIndex: number, newFieldName: string) => {
    setFieldValue(`groupBy.groupings.${groupingIndex}.fields.${fieldIndex}`, newFieldName);
  };

  // const onChangeField = (e: { target: { name: string, value: string } }) => {
  //   const fieldName = e.target.value;
  //   const newField = fieldTypes.all.find((field) => field.name === fieldName);
  //   const newFieldType = newField?.type.type === 'date' ? 'time' : 'values';
  //
  //   if (fieldType !== newFieldType) {
  //     if (newFieldType === 'time') {
  //       setFieldValue(`groupBy.groupings.${groupingIndex}`, {
  //         type: newFieldType,
  //         fields: ['action', 'controller'],
  //         interval: {
  //           type: 'auto',
  //           scaling: 1.0,
  //         },
  //       });
  //     }
  //
  //     if (newFieldType === 'values') {
  //       setFieldValue(`groupBy.groupings.${groupingIndex}`, {
  //         type: newFieldType,
  //         fields: ['action', 'controller'],
  //         limit: defaultLimit,
  //       });
  //
  //       setFieldValue(`groupBy.groupings.${groupingIndex}.interval`, undefined, false);
  //
  //       if (!('limit' in grouping) || ('limit' in grouping && numberNotSet(grouping.limit))) {
  //         setFieldValue(`groupBy.groupings.${groupingIndex}.limit`, defaultLimit);
  //       }
  //     }
  //
  //     return;
  //   }
  //
  //   setFieldValue(`groupBy.groupings.${groupingIndex}`, {
  //     ...grouping,
  //     type: newFieldType,
  //     fields: ['action', 'controller'],
  //   });
  // };

  return (
    <FieldsList>
      {grouping.fields.map((fieldName, fieldIndex) => {
        return (
          <ListItem fieldName={fieldName}
                    onChange={(newFieldName) => onChangeFieldName(fieldIndex, newFieldName)}
                    excludedFields={grouping.fields ?? []} />
        );
      })}
    </FieldsList>
  );
};

export default SelectedFieldsList;
