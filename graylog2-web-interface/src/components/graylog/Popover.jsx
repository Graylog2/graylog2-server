import React from 'react';
// eslint-disable-next-line no-restricted-imports
import { Popover as BoostrapPopover } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

function opacify(amount, color) {
  if (color === 'transparent') return color;

  const parsedAlpha = chroma(color).alpha();
  const newAlpha = (parsedAlpha * 100 + parseFloat(amount) * 100) / 100;

  return chroma(color).alpha(newAlpha).css();
}

const StyledPopover = styled(BoostrapPopover)(({ theme }) => {
  const borderColor = chroma(theme.color.gray[10]).alpha(0.2).css();

  return css`
    & {
      background-color: ${theme.color.global.contentBackground};
      border-color: ${borderColor};

      &.top > .arrow {
        border-top-color: ${opacify(0.05, borderColor)};
        border-top-color: ${chroma(borderColor).alpha()};

        &:after {
          border-top-color: ${theme.color.gray[100]};
        }
      }

      &.right > .arrow {
        border-right-color: ${opacify(0.05, borderColor)};

        &:after {
          border-right-color: ${theme.color.gray[100]};
        }
      }

      &.bottom > .arrow {
        border-bottom-color: ${opacify(0.05, borderColor)};

        &:after {
          border-bottom-color: ${theme.color.gray[100]};
        }
      }

      &.left > .arrow {
        border-left-color: ${opacify(0.05, borderColor)};

        &:after {
          border-left-color: ${theme.color.gray[100]};
        }
      }
    }

    .popover-title {
      background-color: ${theme.color.gray[90]};
    }
  `;
});

const Popover = (props) => {
  return <StyledPopover {...props} />;
};

export default Popover;
