// eslint-disable-next-line no-restricted-imports
import { Popover as BoostrapPopover } from 'react-bootstrap';
import styled from 'styled-components';
import { darken, opacify, transparentize } from 'polished';

import teinte from 'theme/teinte';

const borderColor = transparentize(0.8, teinte.primary.tre);
const Popover = styled(BoostrapPopover)`
  && {
    background-color: ${teinte.primary.due};
    border-color: ${borderColor};

    &.top > .arrow {
      border-top-color: ${opacify(0.05, borderColor)};

      &:after {
        border-top-color: ${teinte.primary.due};
      }
    }

    &.right > .arrow {
      border-right-color: ${opacify(0.05, borderColor)};

      &:after {
        border-right-color: ${teinte.primary.due};
      }
    }

    &.bottom > .arrow {
      border-bottom-color: ${opacify(0.05, borderColor)};

      &:after {
        border-bottom-color: ${teinte.primary.due};
      }
    }

    &.left > .arrow {
      border-left-color: ${opacify(0.05, borderColor)};

      &:after {
        border-left-color: ${teinte.primary.due};
      }
    }
  }

  .popover-title {
    background-color: ${darken(0.03, teinte.primary.due)};
  }
`;

export default Popover;
