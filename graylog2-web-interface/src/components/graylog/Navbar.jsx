// eslint-disable-next-line no-restricted-imports
import { Navbar as BootstrapNavbar } from 'react-bootstrap';
import styled from 'styled-components';
import { darken, lighten } from 'polished';

import { breakpoint, teinte } from 'theme';

const Navbar = styled(BootstrapNavbar)`
  &.navbar-default {
    background-color: ${teinte.secondary.due};
    border-color: ${darken(0.065, teinte.secondary.due)};

    .navbar-brand {
      color: ${teinte.tertiary.uno};
      &:hover,
      &:focus {
        color: ${darken(0.1, teinte.tertiary.uno)};
        background-color: transparent;
      }
    }

    .navbar-text {
      color: ${teinte.primary.tre};
    }

    .navbar-nav {
      > li > a,
      > li > .btn-link {
        color: ${teinte.tertiary.uno};

        &:hover,
        &:focus {
          color: ${darken(0.25, teinte.tertiary.uno)};
          background-color: transparent;
        }
      }
      > .active > a {
        &,
        &:hover,
        &:focus {
          color: ${darken(0.15, teinte.tertiary.uno)};
          background-color: ${darken(0.065, teinte.secondary.due)};
        }
      }
      > .disabled > a {
        &,
        &:hover,
        &:focus {
          color: ${darken(0.25, teinte.secondary.tre)};
          background-color: transparent;
        }
      }

      > .open > a {
        &,
        &:hover,
        &:focus {
          color: ${darken(0.15, teinte.tertiary.uno)};
          background-color: ${darken(0.065, teinte.secondary.due)};
        }
      }

      @media (max-width: ${breakpoint.max.sm}) {
        .open .dropdown-menu {
          > li > a,
          > li > .btn-link {
            color: ${teinte.tertiary.uno};
            &:hover,
            &:focus {
              color: ${darken(0.25, teinte.tertiary.uno)};
              background-color: transparent;
            }
          }
          > .active > a {
            &,
            &:hover,
            &:focus {
              color: ${darken(0.15, teinte.tertiary.uno)};
              background-color: ${darken(0.065, teinte.secondary.due)};
            }
          }
          > .disabled > a {
            &,
            &:hover,
            &:focus {
              color: ${teinte.secondary.tre};
              background-color: transparent;
            }
          }
        }
      }
    }

    .navbar-toggle {
      border-color: ${teinte.secondary.tre};
      &:hover,
      &:focus {
        background-color: ${teinte.secondary.tre};
      }
      .icon-bar {
        background-color: ${darken(0.25, teinte.secondary.tre)};
      }
    }

    .navbar-collapse,
    .navbar-form {
      border-color: ${darken(0.065, teinte.secondary.due)};
    }

    .navbar-link {
      color: ${teinte.tertiary.uno};
      &:hover {
        color: ${darken(0.25, teinte.tertiary.uno)};
      }
    }

    .btn-link {
      color: ${teinte.tertiary.uno};
      &:hover,
      &:focus {
        color: ${darken(0.25, teinte.tertiary.uno)};
      }
      &[disabled],
      fieldset[disabled] & {
        &:hover,
        &:focus {
          color: ${teinte.secondary.tre};
        }
      }
    }
  }

  &.navbar-inverse {
    background-color: ${teinte.primary.tre};
    border-color: ${darken(0.15, teinte.primary.tre)};

    .navbar-brand {
      color: ${lighten(0.15, teinte.tertiary.uno)};
      &:hover,
      &:focus {
        color: ${teinte.primary.due};
        background-color: transparent;
      }
    }

    .navbar-text {
      color: ${lighten(0.15, teinte.primary.tre)};
    }

    .navbar-nav {
      > li > a,
      > li > .btn-link {
        color: ${teinte.secondary.due};

        &:hover,
        &:focus {
          color: ${teinte.tertiary.due};
          background-color: transparent;
        }
      }
      > .active > a {
        &,
        &:hover,
        &:focus {
          color: ${teinte.primary.due};
          background-color: ${lighten(0.20, teinte.primary.tre)};
        }
      }
      > .disabled > a {
        &,
        &:hover,
        &:focus {
          color: ${lighten(0.50, teinte.primary.tre)};
          background-color: transparent;
        }
      }

      > .open > a {
        &,
        &:hover,
        &:focus {
          color: ${teinte.primary.due};
          background-color: ${lighten(0.20, teinte.primary.tre)};
        }
      }

      @media (max-width: ${breakpoint.max.sm}) {
        .open .dropdown-menu {
          > .dropdown-header {
            border-color: ${darken(0.10, teinte.primary.tre)};
          }
          .divider {
            background-color: ${lighten(0.20, teinte.primary.tre)};
          }
          > li > a,
          > li > .btn-link {
            color: ${teinte.secondary.due};
            &:hover,
            &:focus {
              color: ${teinte.tertiary.due};
              background-color: transparent;
            }
          }
          > .active > a {
            &,
            &:hover,
            &:focus {
              color: ${teinte.primary.due};
              background-color: ${lighten(0.20, teinte.primary.tre)};
            }
          }
          > .disabled > a {
            &,
            &:hover,
            &:focus {
              color: ${lighten(0.50, teinte.primary.tre)};
              background-color: transparent;
            }
          }
        }
      }
    }

    .navbar-toggle {
      border-color: ${lighten(0.35, teinte.primary.tre)};
      &:hover,
      &:focus {
        background-color: ${lighten(0.35, teinte.primary.tre)};
      }
      .icon-bar {
        background-color: ${teinte.primary.due};
      }
    }

    .navbar-collapse,
    .navbar-form {
      border-color: ${darken(0.07, teinte.primary.tre)};
    }

    .navbar-link {
      color: ${lighten(0.15, teinte.tertiary.uno)};
      &:hover {
        color: ${teinte.primary.due};
      }
    }

    .btn-link {
      color: ${lighten(0.15, teinte.tertiary.uno)};
      &:hover,
      &:focus {
        color: ${teinte.primary.due};
      }
      &[disabled],
      fieldset[disabled] & {
        &:hover,
        &:focus {
          color: ${lighten(0.50, teinte.primary.tre)};
        }
      }
    }
  }
`;

export default Navbar;
