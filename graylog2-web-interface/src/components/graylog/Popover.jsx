/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
// eslint-disable-next-line no-restricted-imports
import { Popover as BootstrapPopover } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import ThemeAndUserProvider from 'contexts/ThemeAndUserProvider';

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
  return (
    <ThemeAndUserProvider>
      <StyledPopover {...props} />
    </ThemeAndUserProvider>
  );
};

/** @component */
export default Popover;
