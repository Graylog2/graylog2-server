// eslint-disable-next-line no-restricted-imports
import { Tooltip as BootstrapTooltip } from 'react-bootstrap';
import styled from 'styled-components';

import { color } from 'theme';
import { readableColor } from 'theme/utils';

const Tooltip = styled(BootstrapTooltip)`
  &.top .tooltip-arrow {
    bottom: 0;
  }
  &.top .tooltip-arrow,
  &.top-left .tooltip-arrow,
  &.top-right .tooltip-arrow {
    border-top-color: ${color.gray[10]};
  }
  &.right .tooltip-arrow {
    border-right-color: ${color.gray[10]};
  }
  &.left .tooltip-arrow {
    border-left-color: ${color.gray[10]};
  }
  &.bottom .tooltip-arrow,
  &.bottom-left .tooltip-arrow,
  &.bottom-right .tooltip-arrow {
    border-bottom-color: ${color.gray[10]};
  }

  .tooltip-inner {
    color: ${readableColor(color.gray[10])};
    background-color: ${color.gray[10]};
  }
`;

export default Tooltip;
