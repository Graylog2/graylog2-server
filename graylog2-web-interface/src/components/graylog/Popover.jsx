// eslint-disable-next-line no-restricted-imports
import { Popover as BoostrapPopover } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import { opacify, transparentize } from 'polished';

const Popover = styled(BoostrapPopover)((props) => {
  const { color } = props.theme;
  const borderColor = transparentize(0.8, color.gray[0]);

  return css`
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
      background-color: ${color.gray[90]};
    }
  `;
});

export default Popover;
