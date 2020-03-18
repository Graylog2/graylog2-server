// eslint-disable-next-line no-restricted-imports
import { Navbar as BootstrapNavbar } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import { darken, lighten } from 'polished';

import { breakpoint, util } from 'theme';

const Navbar = styled(BootstrapNavbar)(({ theme }) => css`
  &.navbar-default {
    background-color: ${theme.color.gray[90]};
    border-color: ${theme.color.gray[80]};

    .navbar-brand {
      color: ${theme.color.variant.info};
      padding: 12px 15px 0 15px;

      &:hover,
      &:focus {
        color: ${theme.color.variant.dark.info};
        background-color: transparent;
      }
    }

    .navbar-text {
      color: ${theme.color.global.textDefault};
    }

    .navbar-nav {
      > li > a,
      > li > .btn-link {
        color: ${theme.color.variant.info};

        &:hover,
        &:focus {
          color: ${theme.color.variant.dark.info};
          background-color: transparent;
        }
      }

      > .active > a {
        &,
        &:hover,
        &:focus {
          color: ${util.readableColor(theme.color.gray[80])};
          background-color: ${theme.color.gray[80]};
        }
      }

      > .disabled > a {
        &,
        &:hover,
        &:focus {
          color: ${theme.color.gray[70]};
          background-color: transparent;
        }
      }

      > .open > a {
        &,
        &:hover,
        &:focus {
          color: ${darken(0.15, theme.color.variant.info)};
          background-color: ${darken(0.065, theme.color.gray[90])};
        }
      }

      @media (max-width: ${breakpoint.max.sm}) {
        .open .dropdown-menu {
          > li > a,
          > li > .btn-link {
            color: ${theme.color.variant.info};

            &:hover,
            &:focus {
              color: ${darken(0.25, theme.color.variant.info)};
              background-color: transparent;
            }
          }

          > .active > a {
            &,
            &:hover,
            &:focus {
              color: ${darken(0.15, theme.color.variant.info)};
              background-color: ${darken(0.065, theme.color.gray[90])};
            }
          }

          > .disabled > a {
            &,
            &:hover,
            &:focus {
              color: ${theme.color.gray[80]};
              background-color: transparent;
            }
          }
        }
      }
    }

    .navbar-toggle {
      border-color: ${theme.color.gray[80]};

      &:hover,
      &:focus {
        background-color: ${theme.color.gray[80]};
      }

      .icon-bar {
        background-color: ${darken(0.25, theme.color.gray[80])};
      }
    }

    .navbar-collapse,
    .navbar-form {
      border-color: ${darken(0.065, theme.color.gray[90])};
    }

    .navbar-link {
      color: ${theme.color.variant.info};

      &:hover {
        color: ${darken(0.25, theme.color.variant.info)};
      }
    }

    .btn-link {
      color: ${theme.color.variant.info};

      &:hover,
      &:focus {
        color: ${theme.color.variant.dark.info};
      }

      &[disabled],
      fieldset[disabled] & {
        &:hover,
        &:focus {
          color: ${theme.color.gray[80]};
        }
      }
    }
  }

  &.navbar-inverse {
    background-color: ${theme.color.gray[10]};
    border: 0;

    .navbar-brand {
      color: ${lighten(0.15, theme.color.variant.info)};
      padding: 12px 15px 0 15px;

      &:hover,
      &:focus {
        color: ${theme.color.global.textAlt};
        background-color: transparent;
      }
    }

    .navbar-text {
      color: ${lighten(0.15, theme.color.global.textDefault)};
    }

    .navbar-nav {
      > li > a,
      > li > .btn-link {
        color: ${theme.color.gray[90]};

        &:hover,
        &:focus {
          color: ${theme.color.variant.light.info};
          background-color: transparent;
        }
      }

      > .active > a {
        &,
        &:hover,
        &:focus {
          color: ${theme.color.global.textAlt};
          background-color: ${theme.color.gray[20]};
        }
      }

      > .disabled > a {
        &,
        &:hover,
        &:focus {
          color: ${lighten(0.50, theme.color.global.textDefault)};
          background-color: transparent;
        }
      }

      > .open > a {
        &,
        &:hover,
        &:focus {
          color: ${theme.color.global.textAlt};
          background-color: ${theme.color.gray[20]};
        }
      }

      @media (max-width: ${breakpoint.max.sm}) {
        .open .dropdown-menu {
          > .dropdown-header {
            border-color: ${theme.color.gray[10]};
          }

          .divider {
            background-color: ${theme.color.gray[20]};
          }

          > li > a,
          > li > .btn-link {
            color: ${theme.color.gray[90]};

            &:hover,
            &:focus {
              color: ${theme.color.variant.light.info};
              background-color: transparent;
            }
          }

          > .active > a {
            &,
            &:hover,
            &:focus {
              color: ${theme.color.global.textAlt};
              background-color: ${theme.color.gray[20]};
            }
          }

          > .disabled > a {
            &,
            &:hover,
            &:focus {
              color: ${lighten(0.50, theme.color.global.textDefault)};
              background-color: transparent;
            }
          }
        }
      }
    }

    .navbar-toggle {
      border-color: ${theme.color.gray[70]};

      &:hover,
      &:focus {
        background-color: ${theme.color.gray[70]};
      }

      .icon-bar {
        background-color: ${theme.color.global.textAlt};
      }
    }

    .navbar-collapse,
    .navbar-form {
      border-color: ${theme.color.gray[100]};
    }

    .navbar-link {
      color: ${lighten(0.15, theme.color.variant.info)};

      &:hover {
        color: ${theme.color.global.textAlt};
      }
    }

    .btn-link {
      color: ${lighten(0.15, theme.color.variant.info)};

      &:hover,
      &:focus {
        color: ${theme.color.global.textAlt};
      }

      &[disabled],
      fieldset[disabled] & {
        &:hover,
        &:focus {
          color: ${lighten(0.50, theme.color.global.textDefault)};
        }
      }
    }
  }
`);

/** @component */
export default Navbar;
