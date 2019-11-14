// eslint-disable-next-line no-restricted-imports
import { Tooltip as BootstrapTooltip } from 'react-bootstrap';
import styled from 'styled-components';

import { color } from 'theme';

const Tooltip = styled(BootstrapTooltip)`
  &.top .tooltip-arrow {
    bottom: 0;
  }
  &.top .tooltip-arrow,
  &.top-left .tooltip-arrow,
  &.top-right .tooltip-arrow {
    border-top-color: ${color.primary.tre};
  }
  &.right .tooltip-arrow {
    border-right-color: ${color.primary.tre};
  }
  &.left .tooltip-arrow {
    border-left-color: ${color.primary.tre};
  }
  &.bottom .tooltip-arrow,
  &.bottom-left .tooltip-arrow,
  &.bottom-right .tooltip-arrow {
    border-bottom-color: ${color.primary.tre};
  }

  .tooltip-inner {
    color: ${color.primary.due};
    background-color: ${color.primary.tre};
  }
`;

export default Tooltip;
