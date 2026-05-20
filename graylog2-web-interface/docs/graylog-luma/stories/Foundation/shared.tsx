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
import styled from 'styled-components';

export const Token = styled.code`
  font-family: ${({ theme }) => theme.fonts.family.monospace};
  font-size: ${({ theme }) => theme.fonts.size.small};
  color: ${({ theme }) => theme.colors.text.secondary};
  background: rgba(128, 128, 128, 0.12);
  padding: 1px ${({ theme }) => theme.spacings.xs};
  border-radius: 3px;
`;

export const PxLabel = styled.span`
  font-family: ${({ theme }) => theme.fonts.family.monospace};
  font-size: ${({ theme }) => theme.fonts.size.small};
  color: ${({ theme }) => theme.colors.text.secondary};
`;

export const StoryContainer = styled.div`
  padding-bottom: ${({ theme }) => theme.spacings.xl};
`;

export const SectionDescription = styled.p`
  line-height: 1.6;
  margin-bottom: ${({ theme }) => theme.spacings.lg};
`;

// ─── Story Headings ───────────────────────────────────────────────────────

export const H1 = styled.h1`
  margin: 0 0 ${({ theme }) => theme.spacings.md};
`;

export const H2 = styled.h2`
  margin: 0 0 ${({ theme }) => theme.spacings.sm};
`;

export const H3 = styled.h3`
  margin: 0 0 ${({ theme }) => theme.spacings.xs};
`;

// ─── Foundation Table ──────────────────────────────────────────────────────

const StyledTable = styled.table`
  width: 100%;
  border-collapse: collapse;
  font-size: ${({ theme }) => theme.fonts.size.body};
`;

const Th = styled.th<{ $width?: string }>`
  text-align: left;
  padding: ${({ theme }) => `${theme.spacings.xs} ${theme.spacings.sm}`};
  font-size: ${({ theme }) => theme.fonts.size.small};
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: ${({ theme }) => theme.colors.text.secondary};
  border-bottom: 2px solid rgba(128, 128, 128, 0.2);
  ${({ $width }) => $width && `width: ${$width};`}
`;

const Td = styled.td`
  padding: ${({ theme }) => theme.spacings.sm};
  vertical-align: top;
  border-bottom: 1px solid rgba(128, 128, 128, 0.12);
  line-height: 1.5;
`;

type Column<Row> = {
  header: string;
  width?: string;
  render: (row: Row) => React.ReactNode;
};

type FoundationTableProps<Row> = {
  columns: Column<Row>[];
  rows: Row[];
  keyBy: (row: Row) => string;
};

export function FoundationTable<Row>({ columns, rows, keyBy }: FoundationTableProps<Row>) {
  return (
    <StyledTable>
      <thead>
        <tr>
          {columns.map((col) => (
            <Th key={col.header} $width={col.width}>
              {col.header}
            </Th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={keyBy(row)}>
            {columns.map((col) => (
              <Td key={col.header}>{col.render(row)}</Td>
            ))}
          </tr>
        ))}
      </tbody>
    </StyledTable>
  );
}
