// @flow strict
import * as React from 'react';
import { FixedSizeList } from 'react-window';
import { List as ImmutableList } from 'immutable';

import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';
import type { ViewMetaData as ViewMetadata } from 'views/stores/ViewMetadataStore';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

import ListItem, { type ListItemStyle } from './ListItem';

type Props = {
  allFields: ImmutableList<FieldTypeMapping>,
  currentGroup: string,
  fields: ImmutableList<FieldTypeMapping>,
  filter: ?string,
  listHeight: number,
  viewMetadata: ViewMetadata,
};

const isReservedField = (fieldName) => MessageFieldsFilter.FILTERED_FIELDS.includes(fieldName);

const _fieldsToShow = (fields, allFields, currentGroup = 'all') => {
  const isNotReservedField = (f) => !isReservedField(f.name);
  switch (currentGroup) {
    case 'all':
      return allFields.filter(isNotReservedField);
    case 'allreserved':
      return allFields;
    case 'current':
    default:
      return fields.filter(isNotReservedField);
  }
};

const List = ({ viewMetadata: { activeQuery }, listHeight, filter, fields, allFields, currentGroup }: Props) => {
  if (!fields) {
    return <span>No field information available.</span>;
  }
  const fieldFilter = filter ? ((field) => field.name.toLocaleUpperCase().includes(filter.toLocaleUpperCase())) : () => true;
  const fieldsToShow = _fieldsToShow(fields, allFields, currentGroup);
  const fieldList = fieldsToShow
    .filter(fieldFilter)
    .sortBy((field) => field.name.toLocaleUpperCase());

  if (fieldList.isEmpty()) {
    return <i>No fields to show. Try changing your filter term or select a different field set above.</i>;
  }

  const Row = ({ index, style }: { index: number, style: ListItemStyle }) => (
    <ListItem fieldType={fieldList.get(index)}
              selectedQuery={activeQuery}
              fields={fields}
              style={style} />
  );

  return (
    <FixedSizeList height={listHeight}
                   itemCount={fieldList.size}
                   itemSize={17}>
      {Row}
    </FixedSizeList>

  );
};

export default List;
