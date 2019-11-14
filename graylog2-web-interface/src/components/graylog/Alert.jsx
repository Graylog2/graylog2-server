import React, { forwardRef, useMemo } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import { darken, lighten } from 'polished';
// eslint-disable-next-line no-restricted-imports
import { Alert as BootstrapAlert } from 'react-bootstrap';

import contrastingColor from 'util/contrastingColor';
import bsStyleThemeVariant from './variants/bsStyle';

const bsStyleVariants = ['danger', 'info', 'success', 'warning'];

const alertStyles = (hex) => {
  const lightenBorder = lighten(0.30, hex);
  const borderColor = lightenBorder === '#fff' ? darken(0.08, hex) : lightenBorder;

  const lightenBackground = lighten(0.40, hex);
  const backgroundColor = lightenBackground === '#fff' ? darken(0.05, hex) : lightenBackground;

  const textColor = contrastingColor(backgroundColor);

  return css`
    background-color: ${backgroundColor};
    border-color: ${borderColor};
    color: ${textColor};
  `;
};

const Alert = forwardRef(({ bsStyle, ...props }, ref) => {
  const StyledAlert = useMemo(
    () => styled(BootstrapAlert)`${bsStyleThemeVariant(alertStyles)}`,
    [bsStyle],
  );

  return (
    <StyledAlert bsStyle={bsStyle} ref={ref} {...props} />
  );
});

Alert.propTypes = {
  /* Bootstrap `bsStyle` variant name */
  bsStyle: PropTypes.oneOf(bsStyleVariants),
};

Alert.defaultProps = {
  bsStyle: 'info',
};

export default Alert;
