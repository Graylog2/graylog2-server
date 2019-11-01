import React, { forwardRef, useCallback } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { ControlLabel as BootstrapControlLabel } from 'react-bootstrap';

import { useTheme } from 'theme/GraylogThemeContext';

const StyledControlLabel = color => useCallback(styled(BootstrapControlLabel)`
  color: ${color};
  font-weight: bold;
  margin-bottom: 5px;
  display: inline-block;
`, [color]);

const ControlLabelRaw = ({ children, ...props }) => {
  // NOTE: This non-forwarded component is needed for tests in `Enterprise Plugin`
  const { colors } = useTheme();
  const Label = StyledControlLabel(colors.primary.tre);

  return (
    <Label {...props}>{children}</Label>
  );
};

const ControlLabel = forwardRef((props, ref) => <ControlLabelRaw ref={ref} {...props} />);

const propTypes = {
  children: PropTypes.any.isRequired,
};

ControlLabel.propTypes = propTypes;
ControlLabelRaw.propTypes = propTypes;

export default ControlLabel;
export { ControlLabelRaw };
