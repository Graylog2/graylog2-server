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

const Row = styled.tr`
  cursor: default;
`;

const TD = styled.td`
  min-width: 50px;
  word-break: break-word;
  padding: 4px 5px 2px;
`;

const Content = styled.div`
  display: flex;
  align-items: flex-start;
`;

type Props = {
  visibleCellCount: number;
  notice: React.ReactNode;
  actionCell?: React.ReactNode;
};

const EmptyEntityTableRow = ({ visibleCellCount, notice, actionCell = undefined }: Props) => (
  <Row>
    <TD colSpan={Math.max(visibleCellCount - (actionCell ? 1 : 0), 1)}>
      <Content>{notice}</Content>
    </TD>
    {actionCell}
  </Row>
);

export default EmptyEntityTableRow;
