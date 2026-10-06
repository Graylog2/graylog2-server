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
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { fn } from 'storybook/test';

import { Button } from 'components/bootstrap';

const meta = {
  title: 'Components/Buttons/Button',
  component: Button,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    active: { table: { disable: true } },
    allowClickWhenDisabled: { table: { disable: true } },
    'aria-disabled': { table: { disable: true } },
    'aria-label': { table: { disable: true } },
    bsStyle: {
      control: { type: 'select' },
      options: ['danger', 'default', 'info', 'primary', 'success', 'warning', 'gray'],
    },
    bsSize: {
      control: { type: 'select' },
      options: ['xs', 'sm', 'md', 'lg', 'xsmall', 'small', 'large', 'medium'],
    },
    className: { table: { disable: true } },
    'data-testid': { table: { disable: true } },
    form: { table: { disable: true } },
    href: { table: { disable: true } },
    id: { table: { disable: true } },
    name: { table: { disable: true } },
    onBlur: { table: { disable: true } },
    onFocus: { table: { disable: true } },
    onMouseEnter: { table: { disable: true } },
    onMouseLeave: { table: { disable: true } },
    onMouseMove: { table: { disable: true } },
    onPointerDown: { table: { disable: true } },
    onPointerEnter: { table: { disable: true } },
    rel: { table: { disable: true } },
    role: { table: { disable: true } },
    showOverflow: { table: { disable: true } },
    style: { table: { disable: true } },
    tabIndex: { table: { disable: true } },
    target: { table: { disable: true } },
    title: { table: { disable: true } },
    type: { table: { disable: true } },
  },
  args: { onClick: fn() },
} satisfies Meta<typeof Button>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Primary: Story = {
  args: {
    children: 'Primary Button',
    bsStyle: 'primary',
  },
};
