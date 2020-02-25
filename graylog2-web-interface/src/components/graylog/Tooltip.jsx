// eslint-disable-next-line no-restricted-imports
import { Tooltip as BootstrapTooltip } from 'react-bootstrap';
import styled, { css } from 'styled-components';

const Tooltip = styled(BootstrapTooltip)(({ theme }) => css`
  &.top .tooltip-arrow {
    bottom: 0;
  }

  &.top .tooltip-arrow,
  &.top-left .tooltip-arrow,
  &.top-right .tooltip-arrow {
    border-top-color: ${theme.color.primary.tre};
  }

  &.right .tooltip-arrow {
    border-right-color: ${theme.color.primary.tre};
  }

  &.left .tooltip-arrow {
    border-left-color: ${theme.color.primary.tre};
  }

  &.bottom .tooltip-arrow,
  &.bottom-left .tooltip-arrow,
  &.bottom-right .tooltip-arrow {
    border-bottom-color: ${theme.color.primary.tre};
  }

  .tooltip-inner {
    color: ${theme.color.primary.due};
    background-color: ${theme.color.primary.tre};
  }
`);

export default Tooltip;
