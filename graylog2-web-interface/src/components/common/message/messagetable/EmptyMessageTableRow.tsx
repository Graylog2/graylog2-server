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

import { Alert } from 'components/bootstrap';

const Row = styled.tr`
  cursor: default;

  && td {
    min-width: 50px;
    word-break: break-word;
    padding: 4px 5px 2px;
  }
`;

const NoticeCell = styled.td`
  padding: 4px 5px 2px;
`;

const NoticeContent = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
`;

const NoticeAlert = styled(Alert)`
  margin: 0;
  width: 100%;
`;

const NoticeActions = styled.div`
  flex-shrink: 0;
  min-width: 40px;
`;

type Props = {
  colSpan: number;
  messageId: string;
  rowActions?: React.ReactNode;
};

const EmptyMessageTableRow = ({ colSpan, messageId, rowActions = undefined }: Props) => (
  <Row className="table-data-row">
    <NoticeCell colSpan={colSpan}>
      <NoticeContent>
        <NoticeAlert compact>You don&apos;t have access to the message with ID {messageId}.</NoticeAlert>
        {rowActions && <NoticeActions>{rowActions}</NoticeActions>}
      </NoticeContent>
    </NoticeCell>
  </Row>
);

export default EmptyMessageTableRow;
