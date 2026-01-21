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

import { DropdownButton } from 'components/bootstrap';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import MenuItem from 'components/bootstrap/menuitem/MenuItem';
import { defaultCompare } from 'logic/DefaultCompare';

const Container = styled.div`
  min-width: 300px;
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
      <DropdownButton bsSize="small" id="slicing-dropdown" title={activeColumn?.title ?? 'Slice by'}>
        <MenuItem header>Slice by</MenuItem>
        {sliceableColumns.map((schema) => (
          <MenuItem key={schema.id} onClick={() => onChangeSlicing(schema.id)}>
            {schema.title}
          </MenuItem>
        ))}
        <MenuItem divider />
        <MenuItem onClick={() => onChangeSlicing(undefined, undefined)}>No slicing</MenuItem>
      </DropdownButton>
    </Container>
  );
};

export default Slicing;
