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

import { DeleteMenuItem } from 'components/bootstrap';
import OverlayDropdownButton from 'components/common/OverlayDropdownButton';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import MenuItem from 'components/bootstrap/menuitem/MenuItem';
import { defaultCompare } from 'logic/DefaultCompare';

const Container = styled.div`
  min-width: 300px;
`;

const Header = styled.div(
  ({ theme }) => css`
    display: flex;
    gap: ${theme.spacings.xxs};
    align-items: center;
  `,
);

const Headline = styled.h2`
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

type Props = {
  sliceCol: string;
  columnSchemas: Array<ColumnSchema>;
  onChangeSlicing: (sliceCol: string | undefined, slice?: string) => void;
};

const Slicing = ({ sliceCol, columnSchemas, onChangeSlicing }: Props) => {
  const sliceableColumns = columnSchemas
    .filter((schema) => schema.sliceable)
    .sort(({ title: title1 }, { title: title2 }) => defaultCompare(title1, title2));
  const activeColumn = sliceableColumns.find(({ id }) => id === sliceCol);

  return (
    <Container>
      <Header>
        <Headline>{activeColumn.title ?? 'Slice By'}</Headline>
        <OverlayDropdownButton title="Slice by column" buttonTitle="Slice by column" triggerVariant="icon_vertical">
          <MenuItem header>Slice by</MenuItem>
          {sliceableColumns.map((schema) => (
            <MenuItem key={schema.id} onClick={() => onChangeSlicing(schema.id)}>
              {schema.title}
            </MenuItem>
          ))}
          <MenuItem divider />
          <DeleteMenuItem onClick={() => onChangeSlicing(undefined, undefined)}>Remove slicing</DeleteMenuItem>
        </OverlayDropdownButton>
      </Header>
    </Container>
  );
};

export default Slicing;
