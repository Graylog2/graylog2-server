// eslint-disable-next-line no-restricted-imports
import { Navbar as BootstrapNavbar } from 'react-bootstrap';
import styled from 'styled-components';
import { darken, lighten } from 'polished';

import { breakpoint, color } from 'theme';

const Navbar = styled(BootstrapNavbar)`
  &.navbar-default {
    background-color: ${color.gray[90]};
    border-color: ${color.gray[80]};

    .navbar-brand {
      color: ${color.variant.info};
      &:hover,
      &:focus {
        color: ${darken(0.1, color.variant.info)};
        background-color: transparent;
      }
    }

    .navbar-text {
      color: ${color.global.textDefault};
    }

    .navbar-nav {
      > li > a,
      > li > .btn-link {
        color: ${color.variant.info};

        &:hover,
        &:focus {
          color: ${darken(0.25, color.variant.info)};
          background-color: transparent;
        }
      }

      > .active > a {
        &,
        &:hover,
        &:focus {
          color: ${darken(0.15, color.variant.info)};
          background-color: ${color.gray[80]};
        }
      }

      > .disabled > a {
        &,
        &:hover,
        &:focus {
          color: ${darken(0.25, color.gray[80])};
          background-color: transparent;
        }
      }

      > .open > a {
        &,
        &:hover,
        &:focus {
          color: ${darken(0.15, color.variant.info)};
          background-color: ${darken(0.065, color.gray[90])};
        }
      }

      @media (max-width: ${breakpoint.max.sm}) {
        .open .dropdown-menu {
          > li > a,
          > li > .btn-link {
            color: ${color.variant.info};

            &:hover,
            &:focus {
              color: ${darken(0.25, color.variant.info)};
              background-color: transparent;
            }
          }

          > .active > a {
            &,
            &:hover,
            &:focus {
              color: ${darken(0.15, color.variant.info)};
              background-color: ${darken(0.065, color.gray[90])};
            }
          }

          > .disabled > a {
            &,
            &:hover,
            &:focus {
              color: ${color.gray[80]};
              background-color: transparent;
            }
          }
        }
      }
    }

    .navbar-toggle {
      border-color: ${color.gray[80]};

      :hover,
      :focus {
        background-color: ${color.gray[80]};
      }

      .icon-bar {
        background-color: ${darken(0.25, color.gray[80])};
      }
    }

    .navbar-collapse,
    .navbar-form {
      border-color: ${darken(0.065, color.gray[90])};
    }

    .navbar-link {
      color: ${color.variant.info};
      &:hover {
        color: ${darken(0.25, color.variant.info)};
      }
    }

    .btn-link {
      color: ${color.variant.info};

      &:hover,
      &:focus {
        color: ${color.variant.dark.info};
      }

      &[disabled],
      fieldset[disabled] & {
        &:hover,
        &:focus {
          color: ${color.gray[80]};
        }
      }
    }
  }

  &.navbar-inverse {
    background-color: ${color.gray[10]};
    border-color: ${color.gray[100]};

    .navbar-brand {
      color: ${lighten(0.15, color.variant.info)};

      &:hover,
      &:focus {
        color: ${color.global.textAlt};
        background-color: transparent;
      }
    }

    .navbar-text {
      color: ${lighten(0.15, color.global.textDefault)};
    }

    .navbar-nav {
      > li > a,
      > li > .btn-link {
        color: ${color.gray[90]};

        &:hover,
        &:focus {
          color: ${color.variant.light.info};
          background-color: transparent;
        }
      }
      > .active > a {
        &,
        &:hover,
        &:focus {
          color: ${color.global.textAlt};
          background-color: ${color.gray[20]};
        }
      }
      > .disabled > a {
        &,
        &:hover,
        &:focus {
          color: ${lighten(0.50, color.global.textDefault)};
          background-color: transparent;
        }
      }

      > .open > a {
        &,
        &:hover,
        &:focus {
          color: ${color.global.textAlt};
          background-color: ${color.gray[20]};
        }
      }

      @media (max-width: ${breakpoint.max.sm}) {
        .open .dropdown-menu {
          > .dropdown-header {
            border-color: ${color.gray[10]};
          }

          .divider {
            background-color: ${color.gray[20]};
          }

          > li > a,
          > li > .btn-link {
            color: ${color.gray[90]};

            &:hover,
            &:focus {
              color: ${color.variant.light.info};
              background-color: transparent;
            }
          }

          > .active > a {
            &,
            &:hover,
            &:focus {
              color: ${color.global.textAlt};
              background-color: ${color.gray[20]};
            }
          }

          > .disabled > a {
            &,
            &:hover,
            &:focus {
              color: ${lighten(0.50, color.global.textDefault)};
              background-color: transparent;
            }
          }
        }
      }
    }

    .navbar-toggle {
      border-color: ${color.gray[70]};

      &:hover,
      &:focus {
        background-color: ${color.gray[70]};
      }

      .icon-bar {
        background-color: ${color.global.textAlt};
      }
    }

    .navbar-collapse,
    .navbar-form {
      border-color: ${color.gray[100]};
    }

    .navbar-link {
      color: ${lighten(0.15, color.variant.info)};

      &:hover {
        color: ${color.global.textAlt};
      }
    }

    .btn-link {
      color: ${lighten(0.15, color.variant.info)};

      &:hover,
      &:focus {
        color: ${color.global.textAlt};
      }

      &[disabled],
      fieldset[disabled] & {
        &:hover,
        &:focus {
          color: ${lighten(0.50, color.global.textDefault)};
        }
      }
    }
  }
`;

export default Navbar;
