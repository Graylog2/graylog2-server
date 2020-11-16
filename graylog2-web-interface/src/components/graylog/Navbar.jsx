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
import { Navbar as BootstrapNavbar } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

const Navbar = styled(BootstrapNavbar)(({ theme }) => css`
  background-color: ${theme.colors.global.navigationBackground};
  border: 0;
  box-shadow: 0 3px 3px ${theme.colors.global.navigationBoxShadow};

  .navbar-brand {
    color: ${theme.colors.variant.default};
    padding: 12px 15px 0 15px;

    &:hover,
    &:focus {
      color: ${theme.colors.variant.dark.default};
      background-color: transparent;
    }
  }

  .navbar-text {
    color: ${theme.colors.global.textDefault};
  }

  .navbar-nav {
    > li > a,
    > li > .btn-link {
      color: ${theme.colors.global.textDefault};

      &:hover,
      &:focus {
        color: ${theme.colors.variant.darker.default};
        background-color: ${theme.colors.variant.lightest.default};
      }
    }

    > .active > a,
    > .active > .btn-link {
      color: ${theme.colors.variant.darkest.default};
      background-color: ${theme.colors.gray[90]};
      
      &:hover,
      &:focus {
        color: ${theme.colors.variant.darkest.default};
        background-color: ${theme.colors.gray[80]};
      }
    }

    > .disabled > a,
    > .disabled > .btn-link {
      &,
      &:hover,
      &:focus {
        color: ${theme.colors.gray[70]};
        background-color: transparent;
      }
    }

    > .open > a,
    > .open > .btn-link {
      &,
      &:hover,
      &:focus {
        color: ${theme.colors.variant.darkest.default};
        background-color: ${theme.colors.gray[90]};
      }
    }

    @media (max-width: ${theme.breakpoints.max.md}) {
      padding-left: 50px;

      > li > a,
      > li > .btn-link {
        &:hover,
        &:focus {
          color: ${theme.colors.variant.darker.default};
          background-color: ${theme.colors.variant.lightest.default};
        }
      }

      .open .dropdown-menu {
        > li > a,
        > li > .btn-link {
          color: ${theme.colors.variant.darkest.default};

          &:hover,
          &:focus {
            color: ${theme.colors.variant.darker.default};
            background-color: ${theme.colors.variant.lightest.default};
          }
        }

        > .active > a,
        > .active > .btn-link {
          color: ${theme.colors.variant.darkest.default};
          background-color: ${theme.colors.gray[90]};
          
          &:hover,
          &:focus {
            color: ${theme.colors.variant.darkest.default};
            background-color: ${theme.colors.gray[80]};
          }
        }

        > .disabled > a,
        > .disabled > .btn-link {
          &,
          &:hover,
          &:focus {
            color: ${theme.colors.gray[70]};
            background-color: transparent;
          }
        }
      }
    }
  }

  .navbar-toggle {
    background-color: ${theme.colors.global.navigationBackground};
    border-color: ${theme.colors.variant.dark.default};
    transition: background-color 150ms ease-in-out,
      border-color 150ms ease-in-out;
    position: relative;

    :not(.collapsed) {
      .icon-bar:nth-last-child(1),
      .icon-bar:nth-last-child(3) {
        transition: transform 150ms ease-in 150ms,
          top 150ms ease-in 0s;
      }

      .icon-bar:nth-last-child(1) {
        transform: rotate(-45deg) translate(4px, -4px);
      }

      .icon-bar:nth-last-child(3) {
        transform: rotate(45deg) translate(4px, 4px);
      }

      .icon-bar:nth-last-child(2) {
        transition: opacity 0s ease 150ms;
        opacity: 0;
      }
    }

    .icon-bar {
      background-color: ${theme.colors.variant.default};
      transition: background-color 150ms ease-in-out,
        transform 150ms ease-in 0s,
        opacity 300ms ease-in 0s;
      transform: rotate(0deg);
      position: relative;
      opacity: 1;
    }

    &:hover,
    &:focus,
    :not(.collapsed) {
      background-color: ${theme.colors.variant.lighter.default};
      border-color: ${theme.colors.variant.default};
      
      .icon-bar {
        background-color: ${theme.colors.variant.dark.default};
      }
    }
  }

  .navbar-collapse,
  .navbar-form {
    border-color: ${chroma(theme.colors.gray[90]).darken(0.065)};
  }

  .navbar-link {
    color: ${theme.colors.variant.default};

    &:hover {
      color: ${chroma(theme.colors.variant.default).darken(0.25)};
    }
  }

  .btn-link {
    color: ${theme.colors.variant.default};

    &:hover,
    &:focus {
      color: ${theme.colors.variant.dark.default};
    }

    &[disabled],
    fieldset[disabled] & {
      &:hover,
      &:focus {
        color: ${theme.colors.gray[80]};
      }
    }
  }

  .dropdown-header {
    text-transform: uppercase;
    padding: 0 15px !important;
    font-weight: bold;
  }
`);

/** @component */
export default Navbar;
