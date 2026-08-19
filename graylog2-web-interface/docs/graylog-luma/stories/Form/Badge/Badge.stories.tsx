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
import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Badge } from 'components/bootstrap';
import type { StatusColor, StatusVariant } from 'components/bootstrap/types';

const COLORS: StatusColor[] = ['primary', 'danger', 'success', 'warning', 'gray'];
const VARIANTS: StatusVariant[] = ['light', 'filled'];
const SIZES = ['sm', 'md', 'lg'] as const;

const meta = {
  title: 'Components/Badges/Badge',
  component: Badge,
  parameters: {
    layout: 'centered',
    docs: {
      description: {
        component: [
          'A status badge matching the Luma design system. Use `status={{ color, variant, dot? }}`',
          'for the 5 semantic colors (`primary`, `danger`, `success`, `warning`, `gray`) and the 2 style',
          'variants (`light`, `filled`) — this is additive to the legacy `bsStyle` prop, which every',
          'other Badge/Label consumer in the app keeps using unchanged.',
          '',
          '- Use `variant: \'light\'` for most contexts (table cells, status columns) and `\'filled\'`',
          '  for emphasis (e.g. a "default" marker that should stand out from a plain informational one).',
          '- Set `status.dot: true` for a small color dot instead of an icon (used for toggleable states',
          '  like Streams/Event Definitions).',
          '- `leftIcon`/`rightIcon` accept any `Icon` name and render into Mantine\'s native icon slots.',
          '- Pass `onClick` to make the whole badge a real, keyboard-accessible `<button>` instead of a',
          '  decorative `<span>` — this works identically whether or not `status` is set.',
        ].join('\n'),
      },
    },
  },
  tags: ['autodocs'],
  argTypes: {
    status: { control: false },
    bsSize: {
      control: { type: 'select' },
      options: ['xs', 'sm', 'md', 'lg'],
    },
  },
} satisfies Meta<typeof Badge>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Light: Story = {
  args: {
    children: 'Running',
    status: { color: 'success', variant: 'light' },
  },
};

export const Filled: Story = {
  args: {
    children: 'Running',
    status: { color: 'success', variant: 'filled' },
  },
};

export const WithDot: Story = {
  args: {
    children: 'Running',
    status: { color: 'success', variant: 'light', dot: true },
  },
};

export const WithIcon: Story = {
  args: {
    children: 'Paused',
    status: { color: 'warning', variant: 'light' },
    rightIcon: 'pause',
  },
};

export const Clickable: Story = {
  args: {
    children: 'Running',
    status: { color: 'success', variant: 'light', dot: true },
    rightIcon: 'pause',
    onClick: () => {},
  },
};

export const AllColors: Story = {
  parameters: { controls: { disable: true } },
  render: () => (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, max-content)', gap: '8px 24px', alignItems: 'center' }}>
      {COLORS.flatMap((color) =>
        VARIANTS.map((variant) => (
          <Badge key={`${color}-${variant}`} status={{ color, variant }}>
            {color} / {variant}
          </Badge>
        )),
      )}
    </div>
  ),
};

export const Sizes: Story = {
  parameters: { controls: { disable: true } },
  render: () => (
    <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
      {SIZES.map((size) => (
        <Badge key={size} bsSize={size} status={{ color: 'primary', variant: 'filled', dot: true }}>
          {size}
        </Badge>
      ))}
    </div>
  ),
};
