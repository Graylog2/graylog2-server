import React, { forwardRef, useCallback } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import { darken, lighten } from 'polished';
// eslint-disable-next-line no-restricted-imports
import { Alert as BootstrapAlert } from 'react-bootstrap';

import contrastingColor from 'util/contrastingColor';
import bsStyleThemeVariant, { bsStyles } from './variants/bsStyle';

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
  const StyledAlert = useCallback(styled(BootstrapAlert)`
    ${bsStyleThemeVariant(alertStyles)}
  `, [bsStyle]);

  return (
    <StyledAlert bsStyle={bsStyle} ref={ref} {...props} />
  );
});

Alert.propTypes = {
  /* Bootstrap `bsStyle` variant name */
  bsStyle: PropTypes.oneOf(bsStyles),
};

Alert.defaultProps = {
  bsStyle: 'default',
};

export default Alert;
