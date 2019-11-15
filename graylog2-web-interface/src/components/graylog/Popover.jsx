// eslint-disable-next-line no-restricted-imports
import { Popover as BoostrapPopover } from 'react-bootstrap';
import styled from 'styled-components';
import { darken, opacify, transparentize } from 'polished';

import { color } from 'theme';

const borderColor = transparentize(0.8, color.gray[0]);
const Popover = styled(BoostrapPopover)`
  & {
    background-color: ${color.global.contentBackground};
    border-color: ${borderColor};

    &.top > .arrow {
      border-top-color: ${opacify(0.05, borderColor)};

      &:after {
        border-top-color: ${color.gray[100]};
      }
    }

    &.right > .arrow {
      border-right-color: ${opacify(0.05, borderColor)};

      &:after {
        border-right-color: ${color.gray[100]};
      }
    }

    &.bottom > .arrow {
      border-bottom-color: ${opacify(0.05, borderColor)};

      &:after {
        border-bottom-color: ${color.gray[100]};
      }
    }

    &.left > .arrow {
      border-left-color: ${opacify(0.05, borderColor)};

      &:after {
        border-left-color: ${color.gray[100]};
      }
    }
  }

  .popover-title {
    background-color: ${darken(0.03, color.gray[100])};
  }
`;

export default Popover;
