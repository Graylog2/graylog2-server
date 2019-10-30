import React, { forwardRef, useCallback } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Badge as BootstrapBadge } from 'react-bootstrap';

import badgeStyles from './styles/badge';

const Badge = forwardRef((props, ref) => {
  const { bsStyle } = props;
  const StyledBadge = useCallback(styled(BootstrapBadge)`
    ${badgeStyles(props)}
  `, [bsStyle]);

  return (
    <StyledBadge ref={ref} {...props} />
  );
});

Badge.propTypes = {
  bsStyle: PropTypes.oneOf(['success', 'warning', 'danger', 'info', 'default', 'primary']),
};

Badge.defaultProps = {
  bsStyle: 'default',
};

export default Badge;
