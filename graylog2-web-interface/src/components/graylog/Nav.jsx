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

    &${navTabsStyles} /* stylelint-disable-line */
  }
`);

/** @component */
export default Nav;
