// eslint-disable-next-line no-restricted-imports
import { Tooltip as BootstrapTooltip } from 'react-bootstrap';
import styled from 'styled-components';

import teinte from 'theme/teinte';

const Tooltip = styled(BootstrapTooltip)`
  &.top .tooltip-arrow {
    bottom: 0;
  }

  &.top .tooltip-arrow,
  &.top-left .tooltip-arrow,
  &.top-right .tooltip-arrow {
    border-top-color: ${teinte.primary.tre};
  }

  &.right .tooltip-arrow {
    border-right-color: ${teinte.primary.tre};
  }

  &.left .tooltip-arrow {
    border-left-color: ${teinte.primary.tre};
  }

  &.bottom .tooltip-arrow,
  &.bottom-left .tooltip-arrow,
  &.bottom-right .tooltip-arrow {
    border-bottom-color: ${teinte.primary.tre};
  }

  .tooltip-inner {
    color: ${teinte.primary.due};
    background-color: ${teinte.primary.tre};
  }
`;

export default Tooltip;
