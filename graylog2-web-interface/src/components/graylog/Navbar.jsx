// eslint-disable-next-line no-restricted-imports
import { Navbar as BootstrapNavbar } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import { darken, lighten } from 'polished';

import { breakpoint } from 'theme';

const Navbar = styled(BootstrapNavbar)(({ theme }) => css`
  &.navbar-default {
    background-color: ${theme.color.secondary.due};
    border-color: ${darken(0.065, theme.color.secondary.due)};

    .navbar-brand {
      color: ${theme.color.tertiary.uno};

      &:hover,
      &:focus {
        color: ${darken(0.1, theme.color.tertiary.uno)};
        background-color: transparent;
      }
    }

    .navbar-text {
      color: ${theme.color.primary.tre};
    }

    .navbar-nav {
      > li > a,
      > li > .btn-link {
        color: ${theme.color.tertiary.uno};

        &:hover,
        &:focus {
          color: ${darken(0.25, theme.color.tertiary.uno)};
          background-color: transparent;
        }
      }

      > .active > a {
        &,
        &:hover,
        &:focus {
          color: ${darken(0.15, theme.color.tertiary.uno)};
          background-color: ${darken(0.065, theme.color.secondary.due)};
        }
      }

      > .disabled > a {
        &,
        &:hover,
        &:focus {
          color: ${darken(0.25, theme.color.secondary.tre)};
          background-color: transparent;
        }
      }

      > .open > a {
        &,
        &:hover,
        &:focus {
          color: ${darken(0.15, theme.color.tertiary.uno)};
          background-color: ${darken(0.065, theme.color.secondary.due)};
        }
      }

      @media (max-width: ${breakpoint.max.sm}) {
        .open .dropdown-menu {
          > li > a,
          > li > .btn-link {
            color: ${theme.color.tertiary.uno};

            &:hover,
            &:focus {
              color: ${darken(0.25, theme.color.tertiary.uno)};
              background-color: transparent;
            }
          }

          > .active > a {
            &,
            &:hover,
            &:focus {
              color: ${darken(0.15, theme.color.tertiary.uno)};
              background-color: ${darken(0.065, theme.color.secondary.due)};
            }
          }

          > .disabled > a {
            &,
            &:hover,
            &:focus {
              color: ${theme.color.secondary.tre};
              background-color: transparent;
            }
          }
        }
      }
    }

    .navbar-toggle {
      border-color: ${theme.color.secondary.tre};

      &:hover,
      &:focus {
        background-color: ${theme.color.secondary.tre};
      }

      .icon-bar {
        background-color: ${darken(0.25, theme.color.secondary.tre)};
      }
    }

    .navbar-collapse,
    .navbar-form {
      border-color: ${darken(0.065, theme.color.secondary.due)};
    }

    .navbar-link {
      color: ${theme.color.tertiary.uno};

      &:hover {
        color: ${darken(0.25, theme.color.tertiary.uno)};
      }
    }

    .btn-link {
      color: ${theme.color.tertiary.uno};

      &:hover,
      &:focus {
        color: ${darken(0.25, theme.color.tertiary.uno)};
      }

      &[disabled],
      fieldset[disabled] & {
        &:hover,
        &:focus {
          color: ${theme.color.secondary.tre};
        }
      }
    }
  }

  &.navbar-inverse {
    background-color: ${theme.color.primary.tre};
    border-color: ${darken(0.15, theme.color.primary.tre)};

    .navbar-brand {
      color: ${lighten(0.15, theme.color.tertiary.uno)};

      &:hover,
      &:focus {
        color: ${theme.color.primary.due};
        background-color: transparent;
      }
    }

    .navbar-text {
      color: ${lighten(0.15, theme.color.primary.tre)};
    }

    .navbar-nav {
      > li > a,
      > li > .btn-link {
        color: ${theme.color.secondary.due};

        &:hover,
        &:focus {
          color: ${theme.color.tertiary.due};
          background-color: transparent;
        }
      }

      > .active > a {
        &,
        &:hover,
        &:focus {
          color: ${theme.color.primary.due};
          background-color: ${lighten(0.20, theme.color.primary.tre)};
        }
      }

      > .disabled > a {
        &,
        &:hover,
        &:focus {
          color: ${lighten(0.50, theme.color.primary.tre)};
          background-color: transparent;
        }
      }

      > .open > a {
        &,
        &:hover,
        &:focus {
          color: ${theme.color.primary.due};
          background-color: ${lighten(0.20, theme.color.primary.tre)};
        }
      }

      @media (max-width: ${breakpoint.max.sm}) {
        .open .dropdown-menu {
          > .dropdown-header {
            border-color: ${darken(0.10, theme.color.primary.tre)};
          }

          .divider {
            background-color: ${lighten(0.20, theme.color.primary.tre)};
          }

          > li > a,
          > li > .btn-link {
            color: ${theme.color.secondary.due};

            &:hover,
            &:focus {
              color: ${theme.color.tertiary.due};
              background-color: transparent;
            }
          }

          > .active > a {
            &,
            &:hover,
            &:focus {
              color: ${theme.color.primary.due};
              background-color: ${lighten(0.20, theme.color.primary.tre)};
            }
          }

          > .disabled > a {
            &,
            &:hover,
            &:focus {
              color: ${lighten(0.50, theme.color.primary.tre)};
              background-color: transparent;
            }
          }
        }
      }
    }

    .navbar-toggle {
      border-color: ${lighten(0.35, theme.color.primary.tre)};

      &:hover,
      &:focus {
        background-color: ${lighten(0.35, theme.color.primary.tre)};
      }

      .icon-bar {
        background-color: ${theme.color.primary.due};
      }
    }

    .navbar-collapse,
    .navbar-form {
      border-color: ${darken(0.07, theme.color.primary.tre)};
    }

    .navbar-link {
      color: ${lighten(0.15, theme.color.tertiary.uno)};

      &:hover {
        color: ${theme.color.primary.due};
      }
    }

    .btn-link {
      color: ${lighten(0.15, theme.color.tertiary.uno)};

      &:hover,
      &:focus {
        color: ${theme.color.primary.due};
      }

      &[disabled],
      fieldset[disabled] & {
        &:hover,
        &:focus {
          color: ${lighten(0.50, theme.color.primary.tre)};
        }
      }
    }
  }
`);

/** @component */
export default Navbar;
