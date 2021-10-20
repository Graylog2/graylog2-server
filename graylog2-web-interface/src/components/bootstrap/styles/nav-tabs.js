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
import { css } from 'styled-components';

const navTabsStyles = css(({ theme }) => css`
  .nav-tabs {
    border-bottom-color: ${theme.colors.variant.default};

    > li {
      > a {
        transition: background-color 150ms ease-in-out;
        color: ${theme.colors.global.textDefault};
        border-color: ${theme.colors.variant.lighter.default} ${theme.colors.variant.lighter.default} ${theme.colors.variant.default};

        &:hover {
          background-color: ${theme.colors.variant.lightest.default};
          color: ${theme.colors.global.textDefault};
        }
      }

      &.active > a {
        &,
        &:hover,
        &:focus {
          color: ${theme.colors.variant.darkest.default};
          background-color: ${theme.colors.global.contentBackground};
          border-color: ${theme.colors.variant.default};
          border-bottom-color: transparent;
        }
      }

      &.disabled > a {
        &,
        &:hover,
        &:focus {
          color: ${theme.colors.gray[60]};
          background-color: ${theme.colors.gray[100]};
          border-color: ${theme.colors.gray[100]} ${theme.colors.gray[100]} ${theme.colors.variant.default};
          cursor: not-allowed;
        }
      }
    }
  }

  &.nav-justified {
    > .active > a {
      &,
      &:hover,
      &:focus {
        border-color: ${theme.colors.variant.default};
      }
    }

    @media (min-width: ${theme.breakpoints.min.sm}) {
      > li > a {
        border-bottom-color: ${theme.colors.variant.default};
      }

      > .active > a,
      > .active > a:hover,
      > .active > a:focus {
        border-bottom-color: ${theme.colors.gray[100]};
      }
    }
  }
`);

export default navTabsStyles;
