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
import camelCase from 'lodash/camelCase';

import useMetaDataContext from 'components/common/EntityDataTable/hooks/useMetaDataContext';

import type { Column, ColumnRenderer, EntityBase } from './types';

const Td = styled.td`
  word-break: break-word;
`;

const TableCell = <Entity extends EntityBase, Meta>({
  column,
  columnRenderer,
  entity,
  entityAttributesAreCamelCase,
}: {
  column: Column
  columnRenderer: ColumnRenderer<Entity, Meta> | undefined,
  entity: Entity,
  entityAttributesAreCamelCase: boolean,
}) => {
  const { meta } = useMetaDataContext<Meta>();
  const attributeKey = entityAttributesAreCamelCase ? camelCase(column.id) : column.id;
  const attributeValue = entity[attributeKey];
  const content = typeof columnRenderer?.renderCell === 'function' ? columnRenderer.renderCell(attributeValue, entity, column, meta) : attributeValue;

  return (<Td>{content}</Td>);
};

export default TableCell;
