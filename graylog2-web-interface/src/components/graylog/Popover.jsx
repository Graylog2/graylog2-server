// eslint-disable-next-line no-restricted-imports
import { Popover as BoostrapPopover } from 'react-bootstrap';
import styled from 'styled-components';
import { darken, opacify, transparentize } from 'polished';

import { color } from 'theme';

const borderColor = transparentize(0.8, color.primary.tre);
const Popover = styled(BoostrapPopover)`
  & {
    background-color: ${color.primary.due};
    border-color: ${borderColor};

    &.top > .arrow {
      border-top-color: ${opacify(0.05, borderColor)};

      &:after {
        border-top-color: ${color.primary.due};
      }
    }

    &.right > .arrow {
      border-right-color: ${opacify(0.05, borderColor)};

      &:after {
        border-right-color: ${color.primary.due};
      }
    }

    &.bottom > .arrow {
      border-bottom-color: ${opacify(0.05, borderColor)};

      &:after {
        border-bottom-color: ${color.primary.due};
      }
    }

    &.left > .arrow {
      border-left-color: ${opacify(0.05, borderColor)};

      &:after {
        border-left-color: ${color.primary.due};
      }
    }
  }

  .popover-title {
    background-color: ${darken(0.03, color.primary.due)};
  }
`;

export default Popover;
