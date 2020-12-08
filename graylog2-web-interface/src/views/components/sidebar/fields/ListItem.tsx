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
import styled, { css } from 'styled-components';
import type { StyledComponent, Styles } from 'styled-components';
import { List } from 'immutable';

import type { ThemeInterface } from 'theme';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import Field from 'views/components/Field';

import FieldTypeIcon from './FieldTypeIcon';

type Props = {
  activeQueryFields: List<FieldTypeMapping>,
  fieldType: FieldTypeMapping,
  selectedQuery: string,
  style: Styles,
};

const StyledListItem: StyledComponent<{}, ThemeInterface, HTMLLIElement> = styled.li(({ theme }) => css`
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
