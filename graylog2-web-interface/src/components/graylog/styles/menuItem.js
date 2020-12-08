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

const menuItemStyles = css(({ theme }) => css`
  .dropdown-menu {
    background-color: ${theme.colors.global.contentBackground};
    box-shadow: 0 3px 3px ${theme.colors.global.navigationBoxShadow};
    
    > li > a {
      color: ${theme.colors.global.textDefault};

      :hover,
      :focus {
        color: ${theme.colors.variant.darker.default};
        background-color: ${theme.colors.variant.lightest.default};
      }
    }

    > .active > a {
      color: ${theme.colors.variant.darker.default};
      background-color: ${theme.colors.variant.lightest.default};

      :hover,
      :focus {
        color: ${theme.colors.variant.darkest.default};
        background-color: ${theme.colors.variant.lighter.default};
      }
    }

    > .disabled > a {
      color: ${theme.colors.variant.dark.default};
      background-color: ${theme.colors.variant.lightest.default};

      :hover,
      :focus {
        color: ${theme.colors.variant.dark.default};
        background-color: ${theme.colors.variant.lightest.default};
      }
    }
  }
`);

export default menuItemStyles;
