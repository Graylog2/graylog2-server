// eslint-disable-next-line no-restricted-imports
import { Navbar as BootstrapNavbar } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

const Navbar = styled(BootstrapNavbar)(({ theme }) => css`
  background-color: ${theme.colors.global.contentBackground};
  border: 0;
  box-shadow: 0 3px 3px rgba(0, 0, 0, 0.25);

  .navbar-brand {
    color: ${theme.colors.variant.info};
    padding: 12px 15px 0 15px;

    &:hover,
    &:focus {
      color: ${theme.colors.variant.dark.info};
      background-color: transparent;
    }
  }

  .navbar-text {
    color: ${theme.colors.global.textDefault};
  }

  .navbar-nav {
    > li > a,
    > li > .btn-link {
      color: ${theme.colors.variant.darker.info};

      &:hover,
      &:focus {
        color: ${theme.colors.variant.info};
        background-color: transparent;
      }
    }

    > .active > a,
    > .active > .btn-link {
      color: ${theme.colors.variant.dark.info};

      &,
      &:hover,
      &:focus {
        color: ${theme.colors.variant.darkest.info};
        background-color: ${theme.colors.variant.lighter.info};
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
        color: ${theme.colors.variant.darker.info};
        background-color: ${theme.colors.variant.lightest.info};
      }
    }

    @media (max-width: ${theme.breakpoints.max.md}) {
      padding-left: 50px;

      > li > a,
      > li > .btn-link {
        &:hover,
        &:focus {
          background-color: ${theme.colors.variant.lightest.info};
        }
      }

      .open .dropdown-menu {
        > li > a,
        > li > .btn-link {
          color: ${theme.colors.variant.info};

          &:hover,
          &:focus {
            color: ${chroma(theme.colors.variant.info).darken(0.25)};
            background-color: ${theme.colors.variant.lightest.info};
          }
        }

        > .active > a,
        > .active > .btn-link {
          &,
          &:hover,
          &:focus {
            color: ${chroma(theme.colors.variant.info).darken(0.15)};
            background-color: ${chroma(theme.colors.gray[90]).darken(0.065)};
          }
        }

        > .disabled > a,
        > .disabled > .btn-link {
          &,
          &:hover,
          &:focus {
            color: ${theme.colors.gray[80]};
            background-color: transparent;
          }
        }
      }
    }
  }

  .navbar-toggle {
    border-color: ${theme.colors.variant.light.default};

    &:hover,
    &:focus {
      background-color: ${theme.colors.variant.lighter.default};
    }

    .icon-bar {
      background-color: ${theme.colors.variant.default};
    }
  }

  .navbar-collapse,
  .navbar-form {
    border-color: ${chroma(theme.colors.gray[90]).darken(0.065)};
  }

  .navbar-link {
    color: ${theme.colors.variant.info};

    &:hover {
      color: ${chroma(theme.colors.variant.info).darken(0.25)};
    }
  }

  .btn-link {
    color: ${theme.colors.variant.info};

    &:hover,
    &:focus {
      color: ${theme.colors.variant.dark.info};
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
