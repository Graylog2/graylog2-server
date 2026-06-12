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
import React, { useState } from 'react';
import styled from 'styled-components';

export const COL_WIDTH_STYLE = '200px';
export const COL_WIDTH_VARIABLE = '220px';
export const COL_WIDTH_SIZE = '140px';

const TokenWrapper = styled.span`
  position: relative;
  display: inline-block;
`;

const StyledToken = styled.code`
  font-family: ${({ theme }) => theme.fonts.family.monospace};
  font-size: ${({ theme }) => theme.fonts.size.small};
  color: ${({ theme }) => theme.colors.text.secondary};
  background: rgba(128, 128, 128, 0.12);
  padding: 1px ${({ theme }) => theme.spacings.xs};
  border-radius: 3px;
  cursor: pointer;
  user-select: none;

  &:hover {
    background: rgba(128, 128, 128, 0.22);
  }
`;

const CopiedHint = styled.span<{ $visible: boolean }>`
  position: absolute;
  bottom: calc(100% + 4px);
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 3px;
  white-space: nowrap;
  pointer-events: none;
  opacity: ${({ $visible }) => ($visible ? 1 : 0)};
  transition: opacity 0.2s;
`;

export const Token = ({ children }: { children: string }) => {
  const [copied, setCopied] = useState(false);

  const handleClick = () => {
    navigator.clipboard.writeText(children);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  return (
    <TokenWrapper>
      <StyledToken onClick={handleClick} title="Click to copy">
        {children}
      </StyledToken>
      <CopiedHint $visible={copied}>Copied!</CopiedHint>
    </TokenWrapper>
  );
};

export const PxLabel = styled.span`
  font-family: ${({ theme }) => theme.fonts.family.monospace};
  font-size: ${({ theme }) => theme.fonts.size.small};
  color: ${({ theme }) => theme.colors.text.secondary};
`;

export const StoryContainer = styled.div`
  padding-bottom: ${({ theme }) => theme.spacings.xs};
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
  border-bottom: 1px solid rgba(128, 128, 128, 0.2);
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
