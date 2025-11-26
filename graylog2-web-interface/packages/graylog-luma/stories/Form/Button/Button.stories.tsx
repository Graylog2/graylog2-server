// import type { Meta, StoryObj } from '@storybook/react-webpack5';
//
// import { Button } from 'components/bootstrap';
//
// const meta = {
//   component: Button,
// } satisfies Meta<typeof Button>;
//
// export default meta;
// type Story = StoryObj<typeof meta>;
//
// export const Primary: Story = {
//   args: {
//     bsStyle: 'primary',
//     children: 'Primary Button',
//   },
// };

import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { fn } from 'storybook/test';

import { Button } from 'components/bootstrap';

const meta = {
  title: 'Components/Bootstrap/Button',
  component: Button,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    bsStyle: {
      control: { type: 'select' },
      options: ['danger', 'default', 'info', 'primary', 'success', 'warning', 'gray'],
    },
    bsSize: {
      control: { type: 'select' },
      options: ['xs', 'sm', 'md', 'lg', 'xsmall', 'small', 'large', 'medium'],
    },
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
