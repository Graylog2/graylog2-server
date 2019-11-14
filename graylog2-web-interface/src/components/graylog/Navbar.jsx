// eslint-disable-next-line no-restricted-imports
import { Navbar as BootstrapNavbar } from 'react-bootstrap';
import styled from 'styled-components';
import { darken, lighten } from 'polished';

import { breakpoint, color } from 'theme';

const Navbar = styled(BootstrapNavbar)`
  &.navbar-default {
    background-color: ${color.secondary.due};
    border-color: ${darken(0.065, color.secondary.due)};

    .navbar-brand {
      color: ${color.tertiary.uno};
      &:hover,
      &:focus {
        color: ${darken(0.1, color.tertiary.uno)};
        background-color: transparent;
      }
    }

    .navbar-text {
      color: ${color.primary.tre};
    }

    .navbar-nav {
      > li > a,
      > li > .btn-link {
        color: ${color.tertiary.uno};

        &:hover,
        &:focus {
          color: ${darken(0.25, color.tertiary.uno)};
          background-color: transparent;
        }
      }
      > .active > a {
        &,
        &:hover,
        &:focus {
          color: ${darken(0.15, color.tertiary.uno)};
          background-color: ${darken(0.065, color.secondary.due)};
        }
      }
      > .disabled > a {
        &,
        &:hover,
        &:focus {
          color: ${darken(0.25, color.secondary.tre)};
          background-color: transparent;
        }
      }

      > .open > a {
        &,
        &:hover,
        &:focus {
          color: ${darken(0.15, color.tertiary.uno)};
          background-color: ${darken(0.065, color.secondary.due)};
        }
      }

      @media (max-width: ${breakpoint.max.sm}) {
        .open .dropdown-menu {
          > li > a,
          > li > .btn-link {
            color: ${color.tertiary.uno};
            &:hover,
            &:focus {
              color: ${darken(0.25, color.tertiary.uno)};
              background-color: transparent;
            }
          }
          > .active > a {
            &,
            &:hover,
            &:focus {
              color: ${darken(0.15, color.tertiary.uno)};
              background-color: ${darken(0.065, color.secondary.due)};
            }
          }
          > .disabled > a {
            &,
            &:hover,
            &:focus {
              color: ${color.secondary.tre};
              background-color: transparent;
            }
          }
        }
      }
    }

    .navbar-toggle {
      border-color: ${color.secondary.tre};
      &:hover,
      &:focus {
        background-color: ${color.secondary.tre};
      }
      .icon-bar {
        background-color: ${darken(0.25, color.secondary.tre)};
      }
    }

    .navbar-collapse,
    .navbar-form {
      border-color: ${darken(0.065, color.secondary.due)};
    }

    .navbar-link {
      color: ${color.tertiary.uno};
      &:hover {
        color: ${darken(0.25, color.tertiary.uno)};
      }
    }

    .btn-link {
      color: ${color.tertiary.uno};
      &:hover,
      &:focus {
        color: ${darken(0.25, color.tertiary.uno)};
      }
      &[disabled],
      fieldset[disabled] & {
        &:hover,
        &:focus {
          color: ${color.secondary.tre};
        }
      }
    }
  }

  &.navbar-inverse {
    background-color: ${color.primary.tre};
    border-color: ${darken(0.15, color.primary.tre)};

    .navbar-brand {
      color: ${lighten(0.15, color.tertiary.uno)};
      &:hover,
      &:focus {
        color: ${color.primary.due};
        background-color: transparent;
      }
    }

    .navbar-text {
      color: ${lighten(0.15, color.primary.tre)};
    }

    .navbar-nav {
      > li > a,
      > li > .btn-link {
        color: ${color.secondary.due};

        &:hover,
        &:focus {
          color: ${color.tertiary.due};
          background-color: transparent;
        }
      }
      > .active > a {
        &,
        &:hover,
        &:focus {
          color: ${color.primary.due};
          background-color: ${lighten(0.20, color.primary.tre)};
        }
      }
      > .disabled > a {
        &,
        &:hover,
        &:focus {
          color: ${lighten(0.50, color.primary.tre)};
          background-color: transparent;
        }
      }

      > .open > a {
        &,
        &:hover,
        &:focus {
          color: ${color.primary.due};
          background-color: ${lighten(0.20, color.primary.tre)};
        }
      }

      @media (max-width: ${breakpoint.max.sm}) {
        .open .dropdown-menu {
          > .dropdown-header {
            border-color: ${darken(0.10, color.primary.tre)};
          }
          .divider {
            background-color: ${lighten(0.20, color.primary.tre)};
          }
          > li > a,
          > li > .btn-link {
            color: ${color.secondary.due};
            &:hover,
            &:focus {
              color: ${color.tertiary.due};
              background-color: transparent;
            }
          }
          > .active > a {
            &,
            &:hover,
            &:focus {
              color: ${color.primary.due};
              background-color: ${lighten(0.20, color.primary.tre)};
            }
          }
          > .disabled > a {
            &,
            &:hover,
            &:focus {
              color: ${lighten(0.50, color.primary.tre)};
              background-color: transparent;
            }
          }
        }
      }
    }

    .navbar-toggle {
      border-color: ${lighten(0.35, color.primary.tre)};
      &:hover,
      &:focus {
        background-color: ${lighten(0.35, color.primary.tre)};
      }
      .icon-bar {
        background-color: ${color.primary.due};
      }
    }

    .navbar-collapse,
    .navbar-form {
      border-color: ${darken(0.07, color.primary.tre)};
    }

    .navbar-link {
      color: ${lighten(0.15, color.tertiary.uno)};
      &:hover {
        color: ${color.primary.due};
      }
    }

    .btn-link {
      color: ${lighten(0.15, color.tertiary.uno)};
      &:hover,
      &:focus {
        color: ${color.primary.due};
      }
      &[disabled],
      fieldset[disabled] & {
        &:hover,
        &:focus {
          color: ${lighten(0.50, color.primary.tre)};
        }
      }
    }
  }
`;

export default Navbar;
