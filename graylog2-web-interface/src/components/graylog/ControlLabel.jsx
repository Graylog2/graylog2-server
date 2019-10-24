import React, { forwardRef, useCallback } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { ControlLabel as BootstrapControlLabel } from 'react-bootstrap';

import { useTheme } from 'theme/GraylogThemeContext';

const ControlLabel = forwardRef(({ children, ...props }, ref) => {
  const { colors } = useTheme();

  const StyledControlLabel = useCallback(styled(BootstrapControlLabel)`
    color: ${colors.primary.tre};
    font-weight: bold;
    margin-bottom: 5px;
    display: inline-block;
  `, []);

  return (
    <StyledControlLabel ref={ref} {...props}>{children}</StyledControlLabel>
  );
});

ControlLabel.propTypes = {
  children: PropTypes.any,
};

ControlLabel.defaultProps = {
  children: undefined,
};

export default ControlLabel;
