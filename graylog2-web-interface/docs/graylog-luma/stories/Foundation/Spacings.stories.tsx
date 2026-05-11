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
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import styled, { useTheme } from 'styled-components';

import { FoundationItem, PxLabel, StoryContainer, Token, TokenRow, UsageNote } from './shared';

// ─── Spacings ──────────────────────────────────────────────────────────────

const SpacingBar = styled.div<{ $height: string }>`
  width: 100%;
  height: ${({ $height }) => $height};
  background-color: #6366f1;
  border-radius: 2px;
  margin-bottom: ${({ theme }) => theme.spacings.xxs};
`;

type SpacingKey = 'xxs' | 'xs' | 'sm' | 'md' | 'lg' | 'xl' | 'xxl';

type SpacingEntry = {
  key: SpacingKey;
  token: string;
  when: string;
};

const SPACINGS: SpacingEntry[] = [
  {
    key: 'xxs',
    token: 'theme.spacings.xxs',
    when: 'Corner radius on interactive elements (buttons, inputs, cards). Border widths. The minimum meaningful visual unit — not for padding or layout gaps.',
  },
  {
    key: 'xs',
    token: 'theme.spacings.xs',
    when: 'Padding within compact elements (chips, badges, icon buttons). Horizontal gap between inline siblings such as an icon and its label.',
  },
  {
    key: 'sm',
    token: 'theme.spacings.sm',
    when: 'Standard padding inside components (inputs, list items, table cells). Vertical gap between a label and its associated control. Gap between tightly related elements within a component.',
  },
  {
    key: 'md',
    token: 'theme.spacings.md',
    when: 'Default gap in flex and grid layouts. Padding inside medium-sized containers. Vertical separation between elements that belong to the same section but are visually distinct.',
  },
  {
    key: 'lg',
    token: 'theme.spacings.lg',
    when: 'Margin between visually distinct groups within a panel. Larger padding for prominent containers such as empty states or dialog content areas.',
  },
  {
    key: 'xl',
    token: 'theme.spacings.xl',
    when: 'Separation between major independent sections on a page. Use sparingly — most layouts do not need this.',
  },
  {
    key: 'xxl',
    token: 'theme.spacings.xxl',
    when: 'Page-level whitespace only. Rarely appropriate in component authoring — prefer structural layout components over manual large margins.',
  },
];

const SpacingsDoc = () => {
  const theme = useTheme();

  return (
    <StoryContainer>
      <h1>Spacings</h1>
      <UsageNote>
        Spacing tokens are for <strong>component authors</strong> building new reusable components. When you are
        composing existing components, their internal spacing is already built in — you do not need to apply these
        manually.
      </UsageNote>
      {SPACINGS.map(({ key, token, when }) => {
        const value = (theme.spacings as Record<string, string>)[key];
        const px = (theme.spacings.px as Record<string, number>)[key];

        return (
          <FoundationItem key={key}>
            <TokenRow>
              <Token>{token}</Token>
              <PxLabel>
                {value} / {px}px
              </PxLabel>
            </TokenRow>
            <SpacingBar $height={value} />
            <UsageNote>
              <strong>Use for: </strong>
              {when}
            </UsageNote>
          </FoundationItem>
        );
      })}
    </StoryContainer>
  );
};

// ─── Meta & Story ──────────────────────────────────────────────────────────

const meta: Meta = {
  title: 'Foundation/Spacings',
  parameters: {
    layout: 'padded',
    docs: {
      description: {
        component:
          'Spacing tokens from `theme.spacings`. Values follow a Fibonacci-based scale — use the token name, never hardcode `px` values. These tokens will be consolidated in a future design system update.',
      },
    },
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Scale: Story = {
  render: SpacingsDoc,
  parameters: {
    docs: {
      description: {
        story:
          'Each bar is rendered at the actual spacing height so the scale is immediately visible. The three most-used tokens in practice are `xs`, `sm`, and `md`.',
      },
    },
  },
};
