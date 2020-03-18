import React from 'react';
// eslint-disable-next-line no-restricted-imports
import { Popover as BoostrapPopover } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import { opacify, transparentize } from 'polished';
import GraylogThemeProvider from 'theme/GraylogThemeProvider';

const StyledPopover = styled(BoostrapPopover)(({ theme }) => {
  const borderColor = transparentize(0.8, theme.color.gray[0]);

  return css`
    background-color: ${theme.color.global.contentBackground};
    border-color: ${borderColor};

    &.top > .arrow {
      border-top-color: ${opacify(0.05, borderColor)};

      &::after {
        border-top-color: ${theme.color.gray[100]};
      }
    }

    &.right > .arrow {
      border-right-color: ${opacify(0.05, borderColor)};

      &::after {
        border-right-color: ${theme.color.gray[100]};
      }
    }

    &.bottom > .arrow {
      border-bottom-color: ${opacify(0.05, borderColor)};

      &::after {
        border-bottom-color: ${theme.color.gray[100]};
      }
    }

    &.left > .arrow {
      border-left-color: ${opacify(0.05, borderColor)};

      &::after {
        border-left-color: ${theme.color.gray[100]};
      }
    }

    .popover-title {
      background-color: ${theme.color.gray[90]};
    }
  `;
});

const Popover = (allProps) => {
  return (
    <GraylogThemeProvider>
      <StyledPopover {...allProps} />
    </GraylogThemeProvider>
  );
};

/** @component */
export default Popover;
