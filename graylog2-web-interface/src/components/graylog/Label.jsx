import React, { forwardRef, useCallback } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Label as BootstrapLabel } from 'react-bootstrap';

import contrastingColor from 'util/contrastingColor';
import bsStyleThemeVariant, { bsStyles } from './variants/bsStyle';

const labelStyles = (hex) => {
  const textColor = contrastingColor(hex);

  return css`
    background-color: ${hex};
    color: ${textColor};
  `;
};

const Label = forwardRef(({ bsStyle, ...props }, ref) => {
  const StyledLabel = useCallback(styled(BootstrapLabel)`
    ${bsStyleThemeVariant(labelStyles)}
  `, [bsStyle]);

  return (
    <StyledLabel bsStyle={bsStyle} ref={ref} {...props} />
  );
});

Label.propTypes = {
  bsStyle: PropTypes.oneOf(bsStyles),
};

Label.defaultProps = {
  bsStyle: 'default',
};

export default Label;
