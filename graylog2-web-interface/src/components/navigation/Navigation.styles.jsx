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
import styled, { css } from 'styled-components';

import { Navbar } from 'components/graylog';

const StyledNavbar = styled(Navbar)(({ theme }) => css`
  .dev-badge-wrap > a {
    padding: 0 !important;
    cursor: default;
  }

  .dev-badge-wrap .dev-badge {
    margin: 0 10px;
  }

  @media (max-width: 991px) {
    .small-scrn-badge {
      float: right;
      margin: 15px 15px 0;
    }

    .header-meta-nav {
      border-top: 1px solid ${theme.colors.gray[50]};
      padding-top: 7.5px;

      #scratchpad-toggle {
        padding: 10px 15px;
        line-height: 20px;
        display: block;
        width: 100%;
        text-align: left;

        &:hover {
          text-decoration: none;
        }
      }

      #scratchpad-toggle,
      .dropdown-toggle {
        font-size: ${theme.fonts.size.body};
        font-family: ${theme.fonts.family.body};

        &::before {
          content: attr(aria-label);
        }

        [class*="fa-"] {
          display: none;
        }
      }
    }

    .dev-badge-wrap {
      display: none !important;
    }
  }

  @media (min-width: 768px) {
    .navbar-toggle {
      display: block;
    }

    .navbar-collapse {
      width: auto;
      border-top: 1px solid transparent;
      box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1);

      &.collapse {
        height: auto !important;
        padding-bottom: 0;
      }

      &.in {
        overflow-y: auto;
      }
    }

    .navbar-fixed-top .navbar-collapse,
    .navbar-static-top .navbar-collapse,
    .navbar-fixed-bottom .navbar-collapse {
      padding-right: 15px;
      padding-left: 15px;
    }

    .navbar-header {
      float: none;
    }

    .container > .navbar-header,
    .container-fluid > .navbar-header,
    .container > .navbar-collapse,
    .container-fluid > .navbar-collapse {
      margin-right: -15px;
    }
  }

  @media (min-width: 991px) {
    .header-meta-nav {
      display: flex;
      align-items: center;

      #scratchpad-toggle,
      .dropdown-toggle {
        padding: 15px 12px !important;
      }
    }

    .navbar-toggle {
      display: none;
    }

    .navbar-collapse {
      width: auto;
      border-top: 0;
      box-shadow: none;

      &.collapse {
        display: block !important;
        height: auto !important;
        padding-bottom: 0;
        overflow: visible !important;
      }

      &.in {
        overflow-y: visible;
      }
    }

    .navbar-fixed-top .navbar-collapse,
    .navbar-static-top .navbar-collapse,
    .navbar-fixed-bottom .navbar-collapse {
      padding-right: 0;
      padding-left: 0;
    }

    .navbar-header {
      float: left;
    }

    .small-scrn-badge {
      display: none;
    }

    .container > .navbar-header,
    .container-fluid > .navbar-header,
    .container > .navbar-collapse,
    .container-fluid > .navbar-collapse {
      margin-right: 0;
      margin-left: 0;
    }
  }
`);

export default StyledNavbar;
