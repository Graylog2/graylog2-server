import React, { forwardRef, useMemo } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Alert as BootstrapAlert } from 'react-bootstrap';

import { util } from 'theme';
import bsStyleThemeVariant from './variants/bsStyle';

const styleVariants = ['danger', 'info', 'success', 'warning'];

const alertStyles = (hex) => {
  const borderLevel = 6;
  const lightenBorder = util.colorLevel(hex, -borderLevel);
  const borderColor = lightenBorder === '#fff' ? util.colorLevel(hex, borderLevel) : lightenBorder;

  const backgroundLevel = 8.5;
  const lightenBackground = util.colorLevel(hex, -backgroundLevel);
  const backgroundColor = lightenBackground === '#fff' ? util.colorLevel(hex, backgroundLevel) : lightenBackground;

  return css`
    background-color: ${backgroundColor};
    border-color: ${borderColor};
    color: ${util.readableColor(backgroundColor)};
  `;
};


const Alert = forwardRef(({ bsStyle, ...props }, ref) => {
  const StyledAlert = useMemo(
    () => styled(BootstrapAlert)`${bsStyleThemeVariant(alertStyles, {}, styleVariants)}`,
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
