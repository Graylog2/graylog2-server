import { Popover as MantinePopover } from '@mantine/core';
import styled, { css } from 'styled-components';

const StyledPopoverDropdown = styled(MantinePopover.Dropdown)(({ theme }) => css`
  background-color: ${theme.colors.global.contentBackground};
  border-color: ${theme.colors.variant.light.default};
  .mantine-Popover-arrow {
    border-color: ${theme.colors.variant.light.default};
  }
`);

MantinePopover.Dropdown = StyledPopoverDropdown;

export default MantinePopover;
