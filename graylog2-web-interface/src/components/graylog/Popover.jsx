import React from 'react';
// eslint-disable-next-line no-restricted-imports
import { Popover as BoostrapPopover } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import GraylogThemeProvider from 'theme/GraylogThemeProvider';

const StyledPopover = styled(BoostrapPopover)(({ theme }) => {
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
      transform: translate(-50%, -100%);
    
      > .arrow {
        border-top-color: ${borderColor};
  
        &::after {
          border-top-color: ${arrowColor};
        }
      }
    }

    &.right {
      transform: translateY(-50%);
      
      > .arrow {
        border-right-color: ${borderColor};
  
        &::after {
          border-right-color: ${backgroundColor};
          z-index: 1;
        }
      }
    }

    &.bottom {
      transform: translateX(-50%);
      
      > .arrow {
        border-bottom-color: ${borderColor};
  
        &::after {
          border-bottom-color: ${arrowColor};
        }
      }
    }

    &.left {
      transform: translate(-100%, -50%);
      
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
  return (
    <GraylogThemeProvider>
      <StyledPopover {...props} />
    </GraylogThemeProvider>
  );
};

/** @component */
export default Popover;
