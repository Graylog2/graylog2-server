// eslint-disable-next-line no-restricted-imports
import { Navbar as BootstrapNavbar } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';


const Navbar = styled(BootstrapNavbar)(({ theme }) => css`
  &.navbar-default {
    background-color: ${theme.colors.gray[90]};
    border-color: ${theme.colors.gray[80]};

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
        color: ${theme.colors.variant.info};

        &:hover,
        &:focus {
          color: ${theme.colors.variant.dark.info};
          background-color: transparent;
        }
      }

      > .active > a {
        &,
        &:hover,
        &:focus {
          color: ${theme.utils.readableColor(theme.colors.gray[80])};
          background-color: ${theme.colors.gray[80]};
        }
      }

      > .disabled > a {
        &,
        &:hover,
        &:focus {
          color: ${theme.colors.gray[70]};
          background-color: transparent;
        }
      }

      > .open > a {
        &,
        &:hover,
        &:focus {
          color: ${chroma(theme.colors.variant.info).darken(0.15)};
          background-color: ${chroma(theme.colors.gray[90]).darken(0.065)};
        }
      }

      @media (max-width: ${theme.breakpoints.max.sm}) {
        .open .dropdown-menu {
          > li > a,
          > li > .btn-link {
            color: ${theme.colors.variant.info};

            &:hover,
            &:focus {
              color: ${chroma(theme.colors.variant.info).darken(0.25)};
              background-color: transparent;
            }
          }

          > .active > a {
            &,
            &:hover,
            &:focus {
              color: ${chroma(theme.colors.variant.info).darken(0.15)};
              background-color: ${chroma(theme.colors.gray[90]).darken(0.065)};
            }
          }

          > .disabled > a {
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
      border-color: ${theme.colors.gray[80]};

      &:hover,
      &:focus {
        background-color: ${theme.colors.gray[80]};
      }

      .icon-bar {
        background-color: ${chroma(theme.colors.gray[80]).darken(0.25)};
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
  }

  &.navbar-inverse {
    background-color: ${theme.colors.gray[10]};
    border: 0;

    .navbar-brand {
      color: ${chroma(theme.colors.variant.info).brighten(0.15)};
      padding: 12px 15px 0 15px;

      &:hover,
      &:focus {
        color: ${theme.colors.global.textAlt};
        background-color: transparent;
      }
    }

    .navbar-text {
      color: ${chroma(theme.colors.global.textDefault).brighten(0.15)};
    }

    .navbar-nav {
      > li > a,
      > li > .btn-link {
        color: ${theme.colors.gray[90]};

        &:hover,
        &:focus {
          color: ${theme.colors.variant.light.info};
          background-color: transparent;
        }
      }

      > .active > a {
        &,
        &:hover,
        &:focus {
          color: ${theme.colors.global.textAlt};
          background-color: ${theme.colors.gray[20]};
        }
      }

      > .disabled > a {
        &,
        &:hover,
        &:focus {
          color: ${chroma(theme.colors.global.textDefault).brighten(0.50)};
          background-color: transparent;
        }
      }

      > .open > a {
        &,
        &:hover,
        &:focus {
          color: ${theme.colors.global.textAlt};
          background-color: ${theme.colors.gray[20]};
        }
      }

      @media (max-width: ${theme.breakpoints.max.sm}) {
        .open .dropdown-menu {
          > .dropdown-header {
            border-color: ${theme.colors.gray[10]};
          }

          .divider {
            background-color: ${theme.colors.gray[20]};
          }

          > li > a,
          > li > .btn-link {
            color: ${theme.colors.gray[90]};

            &:hover,
            &:focus {
              color: ${theme.colors.variant.light.info};
              background-color: transparent;
            }
          }

          > .active > a {
            &,
            &:hover,
            &:focus {
              color: ${theme.colors.global.textAlt};
              background-color: ${theme.colors.gray[20]};
            }
          }

          > .disabled > a {
            &,
            &:hover,
            &:focus {
              color: ${chroma(theme.colors.global.textDefault).brighten(0.50)};
              background-color: transparent;
            }
          }
        }
      }
    }

    .navbar-toggle {
      border-color: ${theme.colors.gray[70]};

      &:hover,
      &:focus {
        background-color: ${theme.colors.gray[70]};
      }

      .icon-bar {
        background-color: ${theme.colors.global.textAlt};
      }
    }

    .navbar-collapse,
    .navbar-form {
      border-color: ${theme.colors.gray[70]};
    }

    .navbar-link {
      color: ${chroma(theme.colors.variant.info).brighten(0.15)};

      &:hover {
        color: ${theme.colors.global.textAlt};
      }
    }

    .btn-link {
      color: ${chroma(theme.colors.variant.info).brighten(0.15)};

      &:hover,
      &:focus {
        color: ${theme.colors.global.textAlt};
      }

      &[disabled],
      fieldset[disabled] & {
        &:hover,
        &:focus {
          color: ${chroma(theme.colors.global.textDefault).brighten(0.50)};
        }
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
