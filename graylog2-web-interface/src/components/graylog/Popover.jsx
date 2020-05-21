import React from 'react';
// eslint-disable-next-line no-restricted-imports
import { Popover as BoostrapPopover } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

import { util } from 'theme';
import GraylogThemeProvider from 'theme/GraylogThemeProvider';

const StyledPopover = styled(BoostrapPopover)(({ theme }) => {
  const borderColor = chroma(theme.colors.gray[10]).alpha(0.2).css();

  return css`
    background-color: ${theme.colors.global.contentBackground};
    border-color: ${borderColor};

    &.top > .arrow {
      border-top-color: ${util.opacify(borderColor, 0.05)};

      &::after {
        border-top-color: ${theme.colors.gray[100]};
      }
    }

    &.right > .arrow {
      border-right-color: ${util.opacify(borderColor, 0.05)};

      &::after {
        border-right-color: ${theme.colors.gray[100]};
      }
    }

    &.bottom > .arrow {
      border-bottom-color: ${util.opacify(borderColor, 0.05)};

      &::after {
        border-bottom-color: ${theme.colors.gray[100]};
      }
    }

    &.left > .arrow {
      border-left-color: ${util.opacify(borderColor, 0.05)};

      &::after {
        border-left-color: ${theme.colors.gray[100]};
      }
    }

    .popover-title {
      background-color: ${theme.colors.gray[90]};
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
