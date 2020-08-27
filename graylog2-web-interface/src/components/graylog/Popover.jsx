import React, { useContext } from 'react';
// eslint-disable-next-line no-restricted-imports
import { Popover as BootstrapPopover } from 'react-bootstrap';
import styled, { css, ThemeContext } from 'styled-components';

import GraylogThemeProvider from 'theme/GraylogThemeProvider';

const StyledPopover = styled(BootstrapPopover)(({ theme }) => {
  const borderColor = theme.colors.variant.light.default;
  const arrowColor = theme.colors.variant.lightest.default;
  const backgroundColor = theme.colors.global.contentBackground;

  return css`
    background-color: ${backgroundColor};
    border-color: ${borderColor};
    padding: 0;

    .popover-title {
      background-color: ${arrowColor};
      border-color: ${borderColor};
      color: ${theme.colors.variant.darkest.default};
    }

    &.top {
      > .arrow {
        border-top-color: ${borderColor};
  
        &::after {
          border-top-color: ${backgroundColor};
        }
      }
    }

    &.right {
      > .arrow {
        border-right-color: ${borderColor};
  
        &::after {
          border-right-color: ${backgroundColor};
          z-index: 1;
        }
      }
    }

    &.bottom {
      > .arrow {
        border-bottom-color: ${borderColor};
  
        &::after {
          border-bottom-color: ${arrowColor};
        }
      }
    }

    &.left {
      > .arrow {
        border-left-color: ${borderColor};
  
        &::after {
          border-left-color: ${backgroundColor};
        }
      }
    }
  `;
});

const Popover = (props) => {
  const theme = useContext(ThemeContext);

  return (
    <GraylogThemeProvider defaultMode={theme?.mode}>
      <StyledPopover {...props} />
    </GraylogThemeProvider>
  );
};

/** @component */
export default Popover;
