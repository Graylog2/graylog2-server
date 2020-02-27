import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Panel as BootstrapPanel } from 'react-bootstrap';
import { adjustHue, darken } from 'polished';

import { util } from 'theme';
import bsStyleThemeVariant from './variants/bsStyle';

const panelVariantStyles = (hex) => {
  const backgroundColor = util.colorLevel(hex, -9);
  const borderColor = darken(0.05, adjustHue(-10, hex));

  return css`
    border-color: ${borderColor};

    & > .panel-heading {
      color: ${util.colorLevel(backgroundColor, 9)};
      background-color: ${backgroundColor};
      border-color: ${borderColor};

      + .panel-collapse > .panel-body {
        border-top-color: ${borderColor};
      }

      .badge {
        color: ${backgroundColor};
        background-color: ${hex};
      }
    }

    & > .panel-footer {
      + .panel-collapse > .panel-body {
        border-bottom-color: ${borderColor};
      }
    }
  `;
};

const StyledPanel = styled(BootstrapPanel)(({ theme }) => css`
  background-color: ${theme.color.primary.due};

  .panel-footer {
    background-color: ${theme.color.secondary.tre};
    border-top-color: ${theme.color.secondary.due};
  }

  .panel-group {
    .panel-heading {
      + .panel-collapse > .panel-body,
      + .panel-collapse > .list-group {
        border-top-color: ${theme.color.secondary.due};
      }
    }

    .panel-footer {
      + .panel-collapse .panel-body {
        border-bottom-color: ${theme.color.secondary.due};
      }
    }
  }

  ${bsStyleThemeVariant(panelVariantStyles)}
`);

/** @component */
export default StyledPanel;
