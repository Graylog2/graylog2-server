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
import { useState, useCallback, forwardRef, useMemo } from 'react';
import styled from 'styled-components';

import { IconButton, SortableList } from 'components/common';
import FieldSelect from 'views/components/aggregationwizard/FieldSelect';
import TextOverflowEllipsis from 'components/common/TextOverflowEllipsis';
import FieldUnit from 'views/components/aggregationwizard/units/FieldUnit';

const ListItemContainer = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 3px;
`;

const EditFieldSelect = styled.div`
  flex: 1;
`;

const FieldTitle = styled(TextOverflowEllipsis)`
  flex: 1;
`;

type ListItemProps = {
  className: string;
  dragHandle: React.ReactNode;
  fieldSelect: React.ComponentType<React.ComponentProps<typeof FieldSelect>>;
  item: { id: string; title: string };
  onChange: (fieldName: string) => void;
  onRemove: () => void;
  selectSize: 'normal' | 'small';
  selectedFields: Array<string>;
  showUnit: boolean;
};

const Actions = styled.div`
  display: flex;
`;

const ListItem = forwardRef<HTMLDivElement, ListItemProps>(
  (
    {
      className,
      dragHandle,
      fieldSelect = FieldSelect,
      item,
      onChange,
      onRemove,
      selectSize,
      selectedFields,
      showUnit,
    }: ListItemProps,
    ref,
  ) => {
    const [isEditing, setIsEditing] = useState(false);

    const _onChange = (newFieldName: string) => {
      onChange(newFieldName);
      setIsEditing(false);
    };

    return (
      <ListItemContainer className={className} ref={ref}>
        {isEditing && (
          <EditFieldSelect
            id="add-field-select"
            as={fieldSelect}
            onChange={_onChange}
            onMenuClose={() => setIsEditing(false)}
            autoFocus
            openMenuOnFocus
            clearable={false}
            size={selectSize}
            excludedFields={selectedFields.filter((fieldName) => fieldName !== item.id)}
            ariaLabel="Fields"
            name="add-field-select"
            value={item.id}
            aria-label={`Edit ${item.title} field`}
          />
        )}

        {!isEditing && (
          <>
            {dragHandle}
            <FieldTitle>{item.title}</FieldTitle>
            <Actions>
              {showUnit && <FieldUnit field={item.title} />}
              <IconButton name="edit_square" title={`Edit ${item.title} field`} onClick={() => setIsEditing(true)} />
              <IconButton name="delete" title={`Remove ${item.title} field`} onClick={onRemove} />
            </Actions>
          </>
        )}
      </ListItemContainer>
    );
  },
);

type Props = {
  displayOverlayInPortal?: boolean;
  fieldSelect?: React.ComponentType<React.ComponentProps<typeof FieldSelect>>;
  onChange: (newSelectedFields: Array<string>) => void;
  selectSize?: 'normal' | 'small';
  selectedFields: Array<string>;
  showUnit?: boolean;
};

const SelectedFieldsList = ({
  selectedFields,
  onChange,
  selectSize = undefined,
  displayOverlayInPortal = false,
  showUnit = false,
  fieldSelect = undefined,
}: Props) => {
  const fieldsForList = useMemo(() => selectedFields?.map((field) => ({ id: field, title: field })), [selectedFields]);

  const onChangeField = useCallback(
    (fieldIndex: number, newFieldName: string) => {
      const newFields = [...selectedFields];
      newFields[fieldIndex] = newFieldName;

      onChange(newFields);
    },
    [onChange, selectedFields],
  );

  const onRemoveField = useCallback(
    (removedFieldName: string) => {
      const newFields = selectedFields.filter((fieldName) => fieldName !== removedFieldName);
      onChange(newFields);
    },
    [onChange, selectedFields],
  );

  const SortableListItem = useCallback(
    (props: {
      item: { id: string; title: string };
      index: number;
      dragHandle: React.ReactNode;
      className: string;
      ref: React.Ref<HTMLDivElement>;
    }) => {
      const { item, index, dragHandle, className, ref } = props;

      return (
        <ListItem
          onChange={(newFieldName) => onChangeField(index, newFieldName)}
          onRemove={() => onRemoveField(item.id)}
          selectSize={selectSize}
          selectedFields={selectedFields ?? []}
          item={item}
          fieldSelect={fieldSelect}
          dragHandle={dragHandle}
          className={className}
          ref={ref}
          showUnit={showUnit}
        />
      );
    },
    [selectSize, selectedFields, fieldSelect, showUnit, onChangeField, onRemoveField],
  );

  const onSortChange = useCallback(
    (newFieldsList: Array<{ id: string; title: string }>) => {
      onChange(newFieldsList.map(({ id }) => id));
    },
    [onChange],
  );

  if (!selectedFields?.length) {
    return null;
  }

  return (
    <SortableList<{ id: string; title: string }>
      items={fieldsForList}
      onMoveItem={onSortChange}
      customListItemRender={SortableListItem}
      displayOverlayInPortal={displayOverlayInPortal}
    />
  );
};

export default SelectedFieldsList;
