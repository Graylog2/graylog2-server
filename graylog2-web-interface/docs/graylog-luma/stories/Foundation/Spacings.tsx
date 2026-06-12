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
import styled, { useTheme } from 'styled-components';

import { COL_WIDTH_SIZE, COL_WIDTH_VARIABLE, FoundationTable, PxLabel, StoryContainer, Token } from './shared';

// ─── Spacings ──────────────────────────────────────────────────────────────

const SpacingBar = styled.div<{ $height: string }>`
  width: 100%;
  height: ${({ $height }) => $height};
  background-color: #6366f1;
  border-radius: 2px;
`;

type SpacingKey = 'xxs' | 'xs' | 'sm' | 'md' | 'lg' | 'xl' | 'xxl';

type SpacingEntry = {
  key: SpacingKey;
  token: string;
  usage: string;
};

const SPACINGS: SpacingEntry[] = [
  {
    key: 'xxs',
    token: 'theme.spacings.xxs',
    usage:
      'Corner radius on interactive elements (buttons, inputs, cards). Border widths. The minimum meaningful visual unit — not for padding or layout gaps.',
  },
  {
    key: 'xs',
    token: 'theme.spacings.xs',
    usage:
      'Padding within compact elements (chips, badges, icon buttons). Horizontal gap between inline siblings such as an icon and its label.',
  },
  {
    key: 'sm',
    token: 'theme.spacings.sm',
    usage:
      'Standard padding inside components (inputs, list items, table cells). Vertical gap between a label and its associated control. Gap between tightly related elements within a component.',
  },
  {
    key: 'md',
    token: 'theme.spacings.md',
    usage:
      'Default gap in flex and grid layouts. Padding inside medium-sized containers. Vertical separation between elements that belong to the same section but are visually distinct.',
  },
  {
    key: 'lg',
    token: 'theme.spacings.lg',
    usage:
      'Margin between visually distinct groups within a panel. Larger padding for prominent containers such as empty states or dialog content areas.',
  },
  {
    key: 'xl',
    token: 'theme.spacings.xl',
    usage: 'Separation between major independent sections on a page. Use sparingly, most layouts do not need this.',
  },
  {
    key: 'xxl',
    token: 'theme.spacings.xxl',
    usage: 'Page-level whitespace only. Rarely appropriate in component authoring.',
  },
];

export const SpacingsDoc = () => {
  const theme = useTheme();

  const columns = [
    {
      header: 'Style',
      width: '120px',
      render: (row: SpacingEntry) => <SpacingBar $height={theme.spacings[row.key]} />,
    },
    {
      header: 'Variable',
      width: COL_WIDTH_VARIABLE,
      render: (row: SpacingEntry) => <Token>{row.token}</Token>,
    },
    {
      header: 'Size',
      width: COL_WIDTH_SIZE,
      render: (row: SpacingEntry) => (
        <PxLabel>
          {theme.spacings[row.key]} / {theme.spacings.px[row.key]}px
        </PxLabel>
      ),
    },
    {
      header: 'Usage',
      render: (row: SpacingEntry) => row.usage,
    },
  ];

  return (
    <StoryContainer>
      <FoundationTable columns={columns} rows={SPACINGS} keyBy={(row) => row.key} />
    </StoryContainer>
  );
};
