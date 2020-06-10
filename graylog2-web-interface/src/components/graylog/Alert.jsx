import React, { forwardRef } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Alert as BootstrapAlert } from 'react-bootstrap';


const StyledAlert = styled(BootstrapAlert)(({ bsStyle, theme }) => {
  if (!bsStyle) {
    return undefined;
  }

  const borderColor = theme.colors.variant.light[bsStyle];
  const backgroundColor = theme.colors.variant.lightest[bsStyle];

  return css`
    background-color: ${backgroundColor};
    border-color: ${borderColor};
    color: ${theme.utils.readableColor(backgroundColor)};

    a:not(.btn) {
      color: ${theme.utils.contrastingColor(backgroundColor, 'AA')};
      font-weight: bold;
      text-decoration: underline;

      &:hover,
      &:focus,
      &:active {
        color: ${theme.utils.contrastingColor(backgroundColor, 'AAA')};
      }

      &:hover,
      &:focus {
        text-decoration: none;
      }
    }
  `;
});

const Alert = forwardRef(({ bsStyle, ...props }, ref) => {
  return (
    <StyledAlert {...props} bsStyle={bsStyle} ref={ref} />
  );
});

Alert.propTypes = {
  /** Bootstrap `bsStyle` variant name */
  bsStyle: PropTypes.oneOf(['danger', 'info', 'success', 'warning']),
};

Alert.defaultProps = {
  bsStyle: 'info',
};

export default Alert;
