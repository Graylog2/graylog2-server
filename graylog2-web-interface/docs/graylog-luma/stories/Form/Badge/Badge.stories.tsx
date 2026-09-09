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
import { fn } from 'storybook/test';

import { Badge } from 'components/bootstrap';
import { BADGE_COLORS, BADGE_VARIANTS, BADGE_SIZES } from 'components/bootstrap/Badge';

const meta = {
  title: 'Components/Badges/Badge',
  component: Badge,
  parameters: {
    layout: 'centered',
    docs: {
      description: {
        component: [
          'A status badge matching the Luma design system. Use `color`/`variant`/`dot` for the 5',
          'semantic colors (`primary`, `danger`, `success`, `warning`, `gray`) and the 2 style variants',
          '(`light`, `filled`) — this is additive to the legacy `bsStyle` prop, which every other',
          'Badge/Label consumer in the app keeps using unchanged.',
          '',
          '- Use `variant="light"` for most contexts (table cells, status columns) and `"filled"` for',
          '  emphasis (e.g. a "default" marker that should stand out from a plain informational one).',
          '- Set `dot` for a small color dot instead of an icon — use it for a *live, actively-changing*',
          "  process state (e.g. Streams' Running/Paused toggle, a system job's running/queued/complete",
          '  status). Leave it off for a *static, stored* attribute (Enabled/Disabled config flags,',
          '  install/edit state, severity) — there nothing is "happening" right now, so the dot reads as',
          '  noise rather than a live signal.',
          "- `leftIcon`/`rightIcon` accept any `Icon` name and render into Mantine's native icon slots.",
          '- Pass `onClick` to make the whole badge a real, keyboard-accessible `<button>` instead of a',
          '  decorative `<span>` — this works identically regardless of color system.',
        ].join('\n'),
      },
    },
  },
  tags: ['autodocs'],
  argTypes: {
    'aria-label': { table: { disable: true } },
    bsSize: { table: { disable: true } },
    bsStyle: { table: { disable: true } },
    children: { control: 'text', description: 'The badge label text' },
    className: { table: { disable: true } },
    color: {
      control: { type: 'select' },
      options: BADGE_COLORS,
      description: 'Semantic color — picks the background/text pair from the theme',
    },
    'data-testid': { table: { disable: true } },
    dot: {
      control: 'boolean',
      description:
        'Shows a small color dot as the left section (overrides leftIcon). Use for live/actively-changing process state only — not static attributes.',
    },
    leftIcon: {
      control: 'text',
      description: 'Icon name shown before the label, e.g. "play_arrow" — ignored when dot is set',
    },
    onClick: { description: 'Renders the badge as a real, keyboard-accessible button and fires on click' },
    onMouseEnter: { table: { disable: true } },
    onMouseLeave: { table: { disable: true } },
    rightIcon: { control: 'text', description: 'Icon name shown after the label, e.g. "pause"' },
    role: { table: { disable: true } },
    size: { control: { type: 'select' }, options: BADGE_SIZES, description: 'Badge size' },
    style: { table: { disable: true } },
    title: { table: { disable: true } },
    uppercase: { table: { disable: true } },
    variant: {
      control: { type: 'select' },
      options: BADGE_VARIANTS,
      description: '"light" (tinted background) or "filled" (solid background)',
    },
  },
} satisfies Meta<typeof Badge>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Light: Story = {
  args: {
    children: 'Running',
    color: 'success',
    variant: 'light',
  },
};

export const Filled: Story = {
  args: {
    children: 'Running',
    color: 'success',
    variant: 'filled',
  },
};

export const WithDot: Story = {
  args: {
    children: 'Running',
    color: 'success',
    variant: 'light',
    dot: true,
  },
};

export const WithIcon: Story = {
  args: {
    children: 'Paused',
    color: 'warning',
    variant: 'light',
    rightIcon: 'pause',
  },
};

export const Clickable: Story = {
  args: {
    children: 'Running',
    color: 'success',
    variant: 'light',
    dot: true,
    rightIcon: 'pause',
    onClick: fn(),
  },
};

export const AllColors: Story = {
  parameters: { controls: { disable: true } },
  render: () => (
    <div
      style={{ display: 'grid', gridTemplateColumns: 'repeat(2, max-content)', gap: '8px 24px', alignItems: 'center' }}>
      {BADGE_COLORS.flatMap((color) =>
        BADGE_VARIANTS.map((variant) => (
          <Badge key={`${color}-${variant}`} color={color} variant={variant}>
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
      {BADGE_SIZES.map((size) => (
        <Badge key={size} size={size} color="primary" variant="filled" dot>
          {size}
        </Badge>
      ))}
    </div>
  ),
};
