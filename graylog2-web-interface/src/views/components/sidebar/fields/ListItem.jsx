// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';
import { List } from 'immutable';

import { type ThemeInterface } from 'theme';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import Field from 'views/components/Field';
import FieldTypeIcon from 'views/components/sidebar/FieldTypeIcon';

export type ListItemStyle = {
  position: string,
  left: number,
  top: number,
  height: number,
  width: string,
};

type Props = {
  activeQueryFields: List<FieldTypeMapping>,
  fieldType: FieldTypeMapping,
  selectedQuery: string,
  style: ListItemStyle,
};

const StyledListItem: StyledComponent<{}, ThemeInterface, HTMLLIElement> = styled.li(({ theme }) => `
  font-size: ${theme.fonts.size.body};
  display: table-row;
  white-space: nowrap;
`);

const ListItem = ({ activeQueryFields, fieldType, selectedQuery, style }: Props) => {
  const { name, type } = fieldType;
  const disabled = !activeQueryFields.find((f) => f.name === name);

  return (
    <StyledListItem style={style}>
      <FieldTypeIcon type={type} />
      {' '}
      <Field queryId={selectedQuery}
             disabled={disabled}
             name={name}
             type={type}>
        {name}
      </Field>
    </StyledListItem>
  );
};

export default ListItem;
