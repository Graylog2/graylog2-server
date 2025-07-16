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
import styled from 'styled-components';

import { Table } from 'components/bootstrap';

const DataTable = styled(Table)`
  width: 100%;
  padding: ${({ theme }) => theme.spacings.xs};
  margin: 0;
  table-layout: fixed;
`;

const TableRow = styled.tr`
  display: flex;
  flex-direction: row;
  width: 100%;
  padding: ${({ theme }) => theme.spacings.sm} ${({ theme }) => theme.spacings.sm};
  border-bottom: 1px solid ${({ theme }) => theme.colors.table.row.divider};
`;

const TableItem = styled.td<{ $label?: boolean; $paragraph?: boolean; $width?: string; $alignText?: string }>`
  color: ${({ $label, theme }) => ($label ? theme.colors.gray[40] : theme.colors.text.primary)};
  white-space: ${({ $paragraph }) => ($paragraph ? 'pre-wrap' : 'nowrap')};
  overflow: hidden;
  text-overflow: ellipsis;
  border: none !important;
  padding: 0 !important;
  width: ${({ $width }) => $width || 'inherit'};
  text-align: ${({ $alignText }) => $alignText || 'inherit'};
`;

DataTable.Row = TableRow;
DataTable.Item = TableItem;

export default DataTable;
