// eslint-disable-next-line no-restricted-imports
import { Panel as BootstrapPanel } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import { color, util } from 'theme';
import bsStyleThemeVariant from './variants/bsStyle';

const panelVariantStyles = (hex, variant) => {
  const backgroundColor = util.colorLevel(color.variant.light[variant], -9);
  const borderColor = util.colorLevel(color.variant.dark[variant], -6);

  return css`
    border-color: ${borderColor};

    & > .panel-heading {
      color: ${util.readableColor(backgroundColor)};
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

const StyledPanel = styled(BootstrapPanel)`
  background-color: ${color.global.contentBackground};

  .panel-footer {
    background-color: ${color.gray[80]};
    border-top-color: ${color.gray[90]};
  }

  .panel-group {
    .panel-heading {
      font-weight: 700;

      + .panel-collapse > .panel-body,
      + .panel-collapse > .list-group {
        border-top-color: ${color.gray[90]};
      }
    }

    .panel-footer {
      + .panel-collapse .panel-body {
        border-bottom-color: ${color.gray[90]};
      }
    }
  }

  ${bsStyleThemeVariant(panelVariantStyles)}
`;

export default StyledPanel;
