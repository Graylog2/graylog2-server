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
// eslint-disable-next-line no-restricted-imports
import { Nav as BootstrapNav } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import navTabsStyles from './styles/nav-tabs';

const Nav = styled(BootstrapNav)(({ theme }) => css`
  &.nav {
    > li {
      > a {
        transition: background-color 150ms ease-in-out;
        font-family: ${theme.fonts.family.navigation};
        font-size: ${theme.fonts.size.navigation};

        &:hover,
        &:focus {
          background-color: ${theme.colors.variant.lightest.default};
        }
      }

      &.disabled > a {
        color: ${theme.colors.variant.light.default};

        &:hover,
        &:focus {
          color: ${theme.colors.variant.light.default};
        }
      }
    }

    .open > a {
      &,
      &:hover,
      &:focus {
        background-color: ${theme.colors.variant.lightest.default};
        border-color: ${theme.colors.variant.primary};
      }
    }

    &.nav-pills {
      > li {
        &.active > a {
          &,
          &:hover,
          &:focus {
            color: ${theme.utils.contrastingColor(theme.colors.global.link)};
            background-color: ${theme.colors.global.link};
          }
        }
      }
    }
    
    &.nav-sm {
      > li > a {
        padding: 8px 12px;
        font-size: 12px;
      }
    };

    &.nav-xs {
      > li > a {
        padding: 2px 6px;
        font-size: 12px;
      }
    };

    &.nav-lg {
      > li > a {
        padding: 14px 18px;
        font-size: 18px;
        line-height: 1.3;
      }
    };

    &${navTabsStyles} /* This is a known non-issue that stylelint won't ignore but will hopefully be patched soon https://github.com/stylelint/stylelint/issues/4574 */
  }
`);

/** @component */
export default Nav;
