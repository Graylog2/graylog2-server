import React from 'react';
// eslint-disable-next-line no-restricted-imports
import { Popover as BoostrapPopover } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import { darken, opacify, transparentize } from 'polished';
import GraylogThemeProvider from 'theme/GraylogThemeProvider';

const StyledPopover = styled(BoostrapPopover)(({ theme }) => {
  const borderColor = transparentize(0.8, theme.color.primary.tre);

  return css`
    background-color: ${theme.color.primary.due};
    border-color: ${borderColor};

    &.top > .arrow {
      border-top-color: ${opacify(0.05, borderColor)};

      &::after {
        border-top-color: ${theme.color.primary.due};
      }
    }

    &.right > .arrow {
      border-right-color: ${opacify(0.05, borderColor)};

      &::after {
        border-right-color: ${theme.color.primary.due};
      }
    }

    &.bottom > .arrow {
      border-bottom-color: ${opacify(0.05, borderColor)};

      &::after {
        border-bottom-color: ${theme.color.primary.due};
      }
    }

    &.left > .arrow {
      border-left-color: ${opacify(0.05, borderColor)};

      &::after {
        border-left-color: ${theme.color.primary.due};
      }
    }

    .popover-title {
      background-color: ${darken(0.03, theme.color.primary.due)};
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
