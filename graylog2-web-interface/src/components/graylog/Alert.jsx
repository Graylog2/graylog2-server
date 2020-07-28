// @flow strict
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Alert as BootstrapAlert } from 'react-bootstrap';

import type { ThemeInterface } from 'theme/types';

const Alert: StyledComponent<{bsStyle: string}, ThemeInterface, typeof BootstrapAlert> = styled(BootstrapAlert)(({ bsStyle = 'default', theme }) => {
  const borderColor = theme.colors.variant.lighter[bsStyle];
  const backgroundColor = theme.colors.variant.lightest[bsStyle];

  return css`
    background-color: ${backgroundColor};
    border-color: ${borderColor};
    color: ${theme.utils.contrastingColor(backgroundColor)};

    a:not(.btn) {
      color: ${theme.utils.contrastingColor(backgroundColor, 'AA')};
      font-weight: bold;
      text-decoration: underline;

      &:hover,
      &:focus,
      &:active {
        color: ${theme.utils.contrastingColor(backgroundColor)};
      }

      &:hover,
      &:focus {
        text-decoration: none;
      }
    }
  `;
});

export default Alert;
