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
import { useFormikContext } from 'formik';
import { useState, useCallback, forwardRef, useMemo, useContext } from 'react';
import type { DraggableProvidedDraggableProps, DraggableProvidedDragHandleProps } from 'react-beautiful-dnd';

import { IconButton, SortableList, Icon } from 'components/common';
import FieldSelect from 'views/components/aggregationwizard/FieldSelect';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';
import TextOverflowEllipsis from 'components/common/TextOverflowEllipsis';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import {
  onGroupingFieldsChange,
} from 'views/components/aggregationwizard/grouping/GroupingElement';

const ListItemContainer = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 3px;
`;

const EditFieldSelect = styled(FieldSelect)`
  flex: 1;
`;

const FieldTitle = styled(TextOverflowEllipsis)`
  flex: 1;
`;

const DragHandle = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 25px;
  margin-right: 5px;
`;

type ListItemProps = {
  item: { id: string, title: string },
  draggableProps: DraggableProvidedDraggableProps;
  dragHandleProps: DraggableProvidedDragHandleProps;
  className: string,
  onChange: (fieldName: string) => void,
  onRemove: () => void,
  selectedFields: Array<string>,
  testIdPrefix: string,
}

const ListItem = forwardRef<HTMLDivElement, ListItemProps>(({
  item,
  dragHandleProps,
  draggableProps,
  className,
  onChange,
  onRemove,
  selectedFields,
  testIdPrefix,
}: ListItemProps, ref) => {
  const [isEditing, setIsEditing] = useState(false);

  const _onChange = (newFieldName: string) => {
    onChange(newFieldName);
    setIsEditing(false);
  };

  return (
    <ListItemContainer className={className} ref={ref} {...(draggableProps ?? {})}>
      {isEditing && (
        <EditFieldSelect id="group-by-add-field"
                         onChange={_onChange}
                         onMenuClose={() => setIsEditing(false)}
                         autoFocus
                         openMenuOnFocus
                         clearable={false}
                         excludedFields={selectedFields.filter((fieldName) => fieldName !== item.id)}
                         ariaLabel="Fields"
                         name="group-by-add-field-select"
                         value={item.id}
                         aria-label={`Edit ${item.title} field`} />
      )}

      {!isEditing && (
        <>
          <DragHandle {...dragHandleProps} data-testid={`${testIdPrefix}-drag-handle`}>
            <Icon name="bars" />
          </DragHandle>
          <FieldTitle>{item.title}</FieldTitle>
          <div>
            <IconButton name="edit" title={`Edit ${item.title} field`} onClick={() => setIsEditing(true)} />
            <IconButton name="trash-alt" title={`Remove ${item.title} field`} onClick={onRemove} />
          </div>
        </>
      )}
    </ListItemContainer>
  );
});

type Props = {
  groupingIndex: number,
};

const SelectedFieldsList = ({ groupingIndex }: Props) => {
  const { setFieldValue, values } = useFormikContext<WidgetConfigFormValues>();
  const fieldTypes = useContext(FieldTypesContext);
  const activeQueryId = useActiveQueryId();

  const grouping = values.groupBy.groupings[groupingIndex];
  const groupingFieldsForList = useMemo(() => grouping.fields?.map((field) => ({ id: field, title: field })), [grouping.fields]);

  const onChangeField = useCallback((fieldIndex: number, newFieldName: string) => {
    const newFields = [...grouping.fields];
    newFields[fieldIndex] = newFieldName;

    onGroupingFieldsChange({
      fieldTypes,
      activeQueryId,
      groupingIndex,
      grouping,
      newFields,
      setFieldValue,
    });
  }, [activeQueryId, fieldTypes, grouping, groupingIndex, setFieldValue]);

  const onRemoveField = useCallback((removedFieldName: string) => {
    const newFields = grouping.fields.filter((fieldName) => fieldName !== removedFieldName);

    onGroupingFieldsChange({
      fieldTypes,
      activeQueryId,
      groupingIndex,
      grouping,
      newFields,
      setFieldValue,
    });
  }, [activeQueryId, fieldTypes, grouping, groupingIndex, setFieldValue]);

  const SortableListItem = useCallback(({ item, index, dragHandleProps, draggableProps, className, ref }) => (
    <ListItem onChange={(newFieldName) => onChangeField(index, newFieldName)}
              onRemove={() => onRemoveField(item.id)}
              selectedFields={grouping.fields ?? []}
              item={item}
              testIdPrefix={`grouping-${groupingIndex}-field-${index}`}
              dragHandleProps={dragHandleProps}
              draggableProps={draggableProps}
              className={className}
              ref={ref} />
  ), [grouping.fields, groupingIndex, onChangeField, onRemoveField]);

  const onSortChange = useCallback((newGroupings: Array<{ id: string, title: string }>) => {
    const groupingsForForm = newGroupings.map(({ id }) => id);
    setFieldValue(`groupBy.groupings.${groupingIndex}.fields`, groupingsForForm);
  }, [groupingIndex, setFieldValue]);

  if (!grouping.fields?.length) {
    return null;
  }

  return (
    <SortableList items={groupingFieldsForList}
                  onMoveItem={onSortChange}
                  customListItemRender={SortableListItem} />
  );
};

export default SelectedFieldsList;
