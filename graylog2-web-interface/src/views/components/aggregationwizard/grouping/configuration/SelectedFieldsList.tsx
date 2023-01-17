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
import { useState, useCallback, forwardRef, useMemo } from 'react';

// import { DEFAULT_LIMIT } from 'views/Constants';
// import parseNumber from 'views/components/aggregationwizard/grouping/parseNumber';
import type { DraggableProvidedDraggableProps, DraggableProvidedDragHandleProps } from 'react-beautiful-dnd';

import { IconButton, SortableList, Icon } from 'components/common';
import FieldSelect from 'views/components/aggregationwizard/FieldSelect';
import type { WidgetConfigFormValues } from 'views/components/aggregationwizard';
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

const Title = styled.div`
  display: flex;
  align-items: center;
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
  excludedFields: Array<string>,
}

const ListItem = forwardRef<HTMLDivElement, ListItemProps>(({
  item,
  dragHandleProps,
  draggableProps,
  className,
  onChange,
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
                         menuIsOpen
                         autoFocus
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
          <Title>
            <DragHandle {...dragHandleProps}>
              <Icon name="bars" />
            </DragHandle>
            {item.title}
          </Title>
          <div>
            <IconButton name="edit" onClick={() => setIsEditing(true)} />
            <IconButton name="trash-alt" />
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

  const onChangeFieldName = useCallback((fieldIndex: number, newFieldName: string) => {
    setFieldValue(`groupBy.groupings.${groupingIndex}.fields.${fieldIndex}`, newFieldName);
  }, [groupingIndex, setFieldValue]);

  const SortableListItem = useCallback(({ item, index, dragHandleProps, draggableProps, className, ref }) => (
    <ListItem onChange={(newFieldName) => onChangeFieldName(index, newFieldName)}
              excludedFields={grouping.fields ?? []}
              item={item}
              dragHandleProps={dragHandleProps}
              draggableProps={draggableProps}
              className={className}
              ref={ref} />
  ), [grouping.fields, onChangeFieldName]);

  const onSortChange = (newGroupings: Array<{ id: string, title: string }>) => {
    const groupingsForForm = newGroupings.map(({ id }) => id);
    setFieldValue(`groupBy.groupings.${groupingIndex}.fields`, groupingsForForm);
  };

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
