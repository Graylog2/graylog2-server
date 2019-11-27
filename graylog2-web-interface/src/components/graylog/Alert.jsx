import React, { forwardRef, useMemo } from 'react';
import PropTypes from 'prop-types';
// eslint-disable-next-line no-restricted-imports
import { Alert as BootstrapAlert } from 'react-bootstrap';
import styled, { css } from 'styled-components';

import { contrastingColor, colorLevel } from 'theme/utils';
import bsStyleThemeVariant from './variants/bsStyle';

const styleVariants = ['danger', 'info', 'success', 'warning'];

const alertStyles = (hex) => {
  const borderColor = colorLevel(hex, -8);
  const backgroundColor = colorLevel(hex, -10);
  const textColor = contrastingColor(backgroundColor);

  return css`
    background-color: ${backgroundColor};
    border-color: ${borderColor};
    color: ${textColor};
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
  bsStyle: PropTypes.oneOf(styleVariants),
};

Alert.defaultProps = {
  bsStyle: 'info',
};

export default Alert;
