// @flow strict
import * as React from 'react';
import { SizeMe } from 'react-sizeme';
import { FixedSizeList } from 'react-window';
import { List as ImmutableList } from 'immutable';
import type { Styles } from 'styled-components';
import styled from 'styled-components';

import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';
import type { ViewMetaData as ViewMetadata } from 'views/stores/ViewMetadataStore';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

import ListItem from './ListItem';

const DEFAULT_HEIGHT_PX = 50;

const DynamicHeight = styled.div`
  overflow: hidden;
`;

type Props = {
  activeQueryFields: ImmutableList<FieldTypeMapping>,
  allFields: ImmutableList<FieldTypeMapping>,
  currentGroup: string,
  filter: ?string,
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

const List = ({ viewMetadata: { activeQuery }, filter, activeQueryFields, allFields, currentGroup }: Props) => {
  if (!activeQueryFields) {
    return <span>No field information available.</span>;
  }

  const fieldFilter = filter ? ((field) => field.name.toLocaleUpperCase().includes(filter.toLocaleUpperCase())) : () => true;
  const fieldsToShow = _fieldsToShow(activeQueryFields, allFields, currentGroup);
  const fieldList = fieldsToShow
    .filter(fieldFilter)
    .sortBy((field) => field.name.toLocaleUpperCase());

  if (fieldList.isEmpty()) {
    return <i>No fields to show. Try changing your filter term or select a different field set above.</i>;
  }

  const Row = ({ index, style }: { index: number, style: Styles }) => (
    <ListItem fieldType={fieldList.get(index)}
              selectedQuery={activeQuery}
              activeQueryFields={activeQueryFields}
              style={style} />
  );

  return (
    <SizeMe monitorHeight refreshRate={100}>
      {({ size: { height, width } }) => (
        <DynamicHeight>
          <FixedSizeList height={height || DEFAULT_HEIGHT_PX}
                         width={width}
                         itemCount={fieldList.size}
                         itemSize={20}>
            {Row}
          </FixedSizeList>
        </DynamicHeight>
      )}
    </SizeMe>
  );
};

export default List;
