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
import type { GroupByFormValues, WidgetConfigFormValues } from 'views/components/aggregationwizard';
import { DEFAULT_LIMIT, DEFAULT_PIVOT_INTERVAL } from 'views/Constants';
import TextOverflowEllipsis from 'components/common/TextOverflowEllipsis';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import { toValuesGrouping, toTimeGrouping } from 'views/components/aggregationwizard/grouping/GroupingElement';
// import FieldTypesContext from 'views/components/contexts/FieldTypesContext';

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
  excludedFields: Array<string>,
}

const ListItem = forwardRef<HTMLDivElement, ListItemProps>(({
  item,
  dragHandleProps,
  draggableProps,
  className,
  onChange,
  onRemove,
  excludedFields,
}: ListItemProps, ref) => {
  const [isEditing, setIsEditing] = useState(false);

  const _onChange = (newFieldName: string) => {
    onChange(newFieldName);
    setIsEditing(false);
  };

  return (
    <ListItemContainer className={className} ref={ref} {...(draggableProps ?? {})}>
      {isEditing && (
        <EditFieldSelect id="group-by-field-select"
                         onChange={_onChange}
                         onMenuClose={() => setIsEditing(false)}
                         autoFocus
                         openMenuOnFocus
                         clearable={false}
                         excludedFields={excludedFields}
                         ariaLabel="Fields"
                         name="group-by-field-create-select"
                         value={item.id}
                         placeholder="Add a field"
                         aria-label="Add a field" />
      )}

      {!isEditing && (
        <>
          <DragHandle {...dragHandleProps}>
            <Icon name="bars" />
          </DragHandle>
          <FieldTitle>{item.title}</FieldTitle>
          <div>
            <IconButton name="edit" onClick={() => setIsEditing(true)} />
            <IconButton name="trash-alt" onClick={onRemove} />
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
  const grouping = values.groupBy.groupings[groupingIndex];
  const groupingsForList = useMemo(() => grouping.fields?.map((field) => ({ id: field, title: field })), [grouping.fields]);
  const fieldTypes = useContext(FieldTypesContext);
  const queryId = useActiveQueryId();

  const onChangeField = useCallback((fieldIndex: number, newFieldName: string) => {
    const newGroupingFields = [...grouping.fields];
    newGroupingFields[fieldIndex] = newFieldName;

    const groupingHasValuesField = fieldTypes.queryFields.get(queryId, fieldTypes.all).some(({ name, type }) => (
      newGroupingFields.includes(name) && type.type !== 'date'),
    );
    const newGroupingType = groupingHasValuesField ? 'values' : 'time';

    if (grouping.type === newGroupingType) {
      setFieldValue(`groupBy.groupings.${groupingIndex}.fields.${fieldIndex}`, newFieldName);

      return;
    }

    if (grouping.type !== newGroupingType) {
      if (newGroupingType === 'values') {
        setFieldValue(
          `groupBy.groupings.${groupingIndex}`,
          { ...toValuesGrouping(grouping as GroupByFormValues), fields: newGroupingFields },
        );
      }

      if (newGroupingType === 'time') {
        setFieldValue(
          `groupBy.groupings.${groupingIndex}`,
          { ...toTimeGrouping(grouping as GroupByFormValues), fields: newGroupingFields },
        );
      }
    }
  }, [fieldTypes.all, fieldTypes.queryFields, grouping, groupingIndex, queryId, setFieldValue]);

  const onRemoveField = useCallback((removedFieldName: string) => {
    const updatedFields = grouping.fields.filter((fieldName) => fieldName !== removedFieldName);

    if (!updatedFields.length) {
      setFieldValue(`groupBy.groupings.${groupingIndex}`, {
        ...toValuesGrouping(grouping as GroupByFormValues),
        fields: [],
      });

      return;
    }

    setFieldValue(`groupBy.groupings.${groupingIndex}.fields`, updatedFields);
  }, [grouping, groupingIndex, setFieldValue]);

  const SortableListItem = useCallback(({ item, index, dragHandleProps, draggableProps, className, ref }) => (
    <ListItem onChange={(newFieldName) => onChangeField(index, newFieldName)}
              onRemove={() => onRemoveField(item.id)}
              excludedFields={grouping.fields ?? []}
              item={item}
              dragHandleProps={dragHandleProps}
              draggableProps={draggableProps}
              className={className}
              ref={ref} />
  ), [grouping.fields, onChangeField, onRemoveField]);

  const onSortChange = useCallback((newGroupings: Array<{ id: string, title: string }>) => {
    const groupingsForForm = newGroupings.map(({ id }) => id);
    setFieldValue(`groupBy.groupings.${groupingIndex}.fields`, groupingsForForm);
  }, [groupingIndex, setFieldValue]);

  if (!grouping.fields?.length) {
    return null;
  }

  return (
    <SortableList items={groupingsForList}
                  onMoveItem={onSortChange}
                  customListItemRender={SortableListItem} />
  );
};

export default SelectedFieldsList;
