import React from 'react';
import PropTypes from 'prop-types';
import { Button as BootstrapButton } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import theme from 'styled-theming';

import { useTheme } from 'theme/GraylogThemeContext';

const Button = ({ bsStyle, ...props }) => {
  const { colors, utility } = useTheme();

  const cssBuilder = (color) => {
    return css`
    && {
      background-color: ${color};
      border-color: ${utility.darken(color, 0.25)};

      :hover {
        background-color: ${utility.darken(color, 0.25)};
        border-color: ${utility.darken(color, 0.5)};
      }
    }`;
  };

  const buttonStyles = theme.variants('mode', 'bsStyle', {
    danger: {
      teinte: cssBuilder(colors.secondary.uno),
      noire: cssBuilder(utility.opposite(colors.secondary.uno)),
    },
    default: {
      teinte: cssBuilder(colors.secondary.due),
      noire: cssBuilder(utility.opposite(colors.secondary.due)),
    },
    info: {
      teinte: cssBuilder(colors.tertiary.uno),
      noire: cssBuilder(utility.opposite(colors.tertiary.uno)),
    },
    primary: {
      teinte: cssBuilder(colors.tertiary.quattro),
      noire: cssBuilder(utility.opposite(colors.tertiary.quattro)),
    },
    success: {
      teinte: cssBuilder(colors.tertiary.tre),
      noire: cssBuilder(utility.opposite(colors.tertiary.tre)),
    },
    warning: {
      teinte: cssBuilder(colors.tertiary.sei),
      noire: cssBuilder(utility.opposite(colors.tertiary.sei)),
    },
  });

  const StyledButton = styled(BootstrapButton)`
    ${buttonStyles};
  `;

  return (
    <StyledButton bsStyle={bsStyle} {...props} />
  );
};

Button.propTypes = {
  /* NOTE: need prop so we can set default style */
  bsStyle: PropTypes.oneOf(['success', 'warning', 'danger', 'info', 'default', 'primary', 'link']),
};

Button.defaultProps = {
  bsStyle: 'default',
};

export default Button;
