import React, { forwardRef, useMemo } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Alert as BootstrapAlert } from 'react-bootstrap';

import { util } from 'theme';
import bsStyleThemeVariant from './variants/bsStyle';

const styleVariants = ['danger', 'info', 'success', 'warning'];

const alertStyles = (hex) => {
  const borderColor = util.colorLevel(hex, -8);
  const backgroundColor = util.colorLevel(hex, -10);

  return css`
    background-color: ${backgroundColor};
    border-color: ${borderColor};
    color: ${util.readableColor(backgroundColor)};

    a:not(.btn) {
      color: ${util.contrastingColor(backgroundColor, 'AA')};
      font-weight: bold;
      text-decoration: underline;

      &:hover,
      &:focus {
        color: ${util.contrastingColor(backgroundColor, 'AAA')};
        text-decoration: none;
      }

      &:active {
        color: ${util.contrastingColor(backgroundColor, 'AAA')};
      }
    }
  `;
};


const Alert = forwardRef(({ bsStyle, ...props }, ref) => {
  const StyledAlert = useMemo(
    () => styled(BootstrapAlert)`
      ${bsStyleThemeVariant(alertStyles, {}, styleVariants)}
    `,
    [bsStyle],
  );

  return (
    <StyledAlert bsStyle={bsStyle} ref={ref} {...props} />
  );
});

Alert.propTypes = {
  /** Bootstrap `bsStyle` variant name */
  bsStyle: PropTypes.oneOf(styleVariants),
};

Alert.defaultProps = {
  bsStyle: 'info',
};

export default Alert;
