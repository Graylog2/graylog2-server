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

import OverlayDropdownButton from 'components/common/OverlayDropdownButton';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import MenuItem from 'components/bootstrap/menuitem/MenuItem';
import { defaultCompare } from 'logic/DefaultCompare';

const Container = styled.div(
  ({ theme }) => css`
    min-width: 300px;
  `,
);

const Header = styled.div(
  ({ theme }) => css`
    display: flex;
    gap: 5px;
    align-items: center;
  `,
);

type Props = {
  sliceCol: string;
  slice: string;
  columnSchemas: Array<ColumnSchema>;
  onChangeSlicing: (sliceCol: string, slice?: string) => void;
};

const Slicing = ({ sliceCol, slice, columnSchemas, onChangeSlicing }: Props) => {
  const sliceableColumns = columnSchemas
    .filter((schema) => schema.sliceable)
    .sort(({ title: title1 }, { title: title2 }) => defaultCompare(title1, title2));
  const activeColumn = sliceableColumns.find(({ id }) => id === sliceCol);

  return (
    <Container>
      <Header>
        <h2>Slice By</h2>
        <OverlayDropdownButton title={activeColumn.title}>
          {sliceableColumns.map((schema) => (
            <MenuItem key={schema.id} onClick={() => onChangeSlicing(schema.id)}>
              {schema.title}
            </MenuItem>
          ))}
        </OverlayDropdownButton>
      </Header>
    </Container>
  );
};

export default Slicing;
