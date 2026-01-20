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
import { List as VirtualList, type RowComponentProps } from 'react-window';
import type { List as ImmutableList } from 'immutable';
import styled from 'styled-components';

import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import ElementDimensions from 'components/common/ElementDimensions';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import isFilteredField from 'views/logic/IsFilteredField';

import ListItem from './ListItem';

const DEFAULT_HEIGHT_PX = 50;

const DynamicHeight = styled(ElementDimensions)`
  overflow: hidden;
`;

type Props = {
  activeQueryFields: ImmutableList<FieldTypeMapping>;
  allFields: ImmutableList<FieldTypeMapping>;
  currentGroup: string;
  filter: string | undefined | null;
};

const _fieldsToShow = (
  fields: ImmutableList<FieldTypeMapping>,
  allFields: ImmutableList<FieldTypeMapping>,
  currentGroup: string = 'all',
): ImmutableList<FieldTypeMapping> => {
  const isNotReservedField = (f: FieldTypeMapping) => !isFilteredField(f.name);

  switch (currentGroup) {
    case 'all':
      return allFields.filter(isNotReservedField).toList();
    case 'allreserved':
      return allFields;
    case 'current':
    default:
      return fields.filter(isNotReservedField).toList();
  }
};

type RowProps = {
  fieldList: ImmutableList<FieldTypeMapping>;
  activeQueryFields: ImmutableList<FieldTypeMapping>;
  selectedQuery: string;
};

const RowComponent = ({ index, style, fieldList, activeQueryFields, selectedQuery }: RowComponentProps<RowProps>) => (
  <ListItem
    fieldType={fieldList.get(index)}
    selectedQuery={selectedQuery}
    activeQueryFields={activeQueryFields}
    style={style}
  />
);

const List = ({ filter, activeQueryFields, allFields, currentGroup }: Props) => {
  const activeQuery = useActiveQueryId();

  if (!activeQueryFields) {
    return <span>No field information available.</span>;
  }

  const fieldFilter = filter
    ? (field: { name: string }) => field.name.toLocaleUpperCase().includes(filter.toLocaleUpperCase())
    : () => true;
  const fieldsToShow = _fieldsToShow(activeQueryFields, allFields, currentGroup);
  const fieldList = fieldsToShow
    .filter(fieldFilter)
    .sortBy((field) => field.name.toLocaleUpperCase())
    .toList();

  if (fieldList.isEmpty()) {
    return <i>No fields to show. Try changing your filter term or select a different field set above.</i>;
  }

  return (
    <DynamicHeight>
      {({ width, height }) => (
        <VirtualList
          style={{ height: height || DEFAULT_HEIGHT_PX, width }}
          rowComponent={RowComponent}
          rowCount={fieldList.size}
          rowHeight={20}
          rowProps={{
            fieldList,
            activeQueryFields,
            selectedQuery: activeQuery,
          }}
        />
      )}
    </DynamicHeight>
  );
};

export default List;
