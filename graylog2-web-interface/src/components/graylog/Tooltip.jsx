// eslint-disable-next-line no-restricted-imports
import { Tooltip as BootstrapTooltip } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import { util } from 'theme';

const Tooltip = styled(BootstrapTooltip)(({ theme }) => css`
  &.top .tooltip-arrow {
    bottom: 0;
  }

  &.top .tooltip-arrow,
  &.top-left .tooltip-arrow,
  &.top-right .tooltip-arrow {
    border-top-color: ${theme.color.gray[10]};
  }

  &.right .tooltip-arrow {
    border-right-color: ${theme.color.gray[10]};
  }

  &.left .tooltip-arrow {
    border-left-color: ${theme.color.gray[10]};
  }

  &.bottom .tooltip-arrow,
  &.bottom-left .tooltip-arrow,
  &.bottom-right .tooltip-arrow {
    border-bottom-color: ${theme.color.gray[10]};
  }

  .tooltip-inner {
    color: ${util.readableColor(theme.color.gray[10])};
    background-color: ${theme.color.gray[10]};
  }
`);

/** @component */
export default Tooltip;
