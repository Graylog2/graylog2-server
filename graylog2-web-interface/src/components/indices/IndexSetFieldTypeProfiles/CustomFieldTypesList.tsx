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
import React from 'react';
import { styled } from 'styled-components';

import type { CustomFieldMapping } from 'components/indices/IndexSetFieldTypeProfiles/types';
import type { FieldTypes } from 'views/logic/fieldactions/ChangeFieldType/types';

const Item = styled.div`
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
`;

const List = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1em;
`;
const CustomFieldTypesList = ({ list, fieldTypes }: { list: Array<CustomFieldMapping>, fieldTypes: FieldTypes }) => (
  <List>
    {list.map(({ field, type }) => (
      <Item key={field}>
        <b>{field}:</b><i>{fieldTypes[type]}</i>
      </Item>
    ))}
  </List>
);

export default CustomFieldTypesList;
