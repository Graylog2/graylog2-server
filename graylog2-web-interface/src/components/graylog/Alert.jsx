import React, { forwardRef, useCallback } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Alert as BootstrapAlert } from 'react-bootstrap';

import alertStyles from './styles/alert';

const Alert = forwardRef(({ bsStyle, ...props }, ref) => {
  const StyledAlert = useCallback(styled(BootstrapAlert)`
    ${alertStyles()}
  `, [bsStyle]);

  return (
    <StyledAlert bsStyle={bsStyle} ref={ref} {...props} />
  );
});

Alert.propTypes = {
  bsStyle: PropTypes.oneOf(['success', 'warning', 'danger', 'info', 'default', 'primary']),
};

Alert.defaultProps = {
  bsStyle: 'default',
};

export default Alert;
