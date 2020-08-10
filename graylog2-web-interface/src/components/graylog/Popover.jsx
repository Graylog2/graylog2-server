import React from 'react';
// eslint-disable-next-line no-restricted-imports
import { Popover as BoostrapPopover } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import GraylogThemeProvider from 'theme/GraylogThemeProvider';

const StyledPopover = styled(BoostrapPopover)(({ theme }) => {
  const borderColor = theme.colors.variant.light.default;
  const arrowColor = theme.colors.variant.lightest.default;

  return css`
    background-color: ${theme.colors.global.contentBackground};
    border-color: ${borderColor};
    padding: 0;

    .popover-title {
      background-color: ${arrowColor};
      border-color: ${borderColor};
      color: ${theme.colors.variant.darkest.default};
    }

    &.top > .arrow {
      border-top-color: ${borderColor};

      &::after {
        border-top-color: ${arrowColor};
      }
    }

    &.right > .arrow {
      border-right-color: ${borderColor};

      &::after {
        border-right-color: ${arrowColor};
      }
    }

    &.bottom > .arrow {
      border-bottom-color: ${borderColor};

      &::after {
        border-bottom-color: ${arrowColor};
      }
    }

    &.left > .arrow {
      border-left-color: ${borderColor};

      &::after {
        border-left-color: ${arrowColor};
      }
    }
  `;
});

const Popover = (props) => {
  return (
    <GraylogThemeProvider>
      <StyledPopover {...props} />
    </GraylogThemeProvider>
  );
};

/** @component */
export default Popover;
