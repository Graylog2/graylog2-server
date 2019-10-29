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

const ControlLabel = forwardRef(({ children, ...props }, ref) => {
  const { colors } = useTheme();
  const Label = StyledControlLabel(colors.primary.tre);

  return (
    <Label ref={ref} {...props}>{children}</Label>
  );
});

const ControlLabelRaw = ({ children, ...props }) => {
  // NOTE: This non-forwarded component is needed for tests in
  // `graylog-plugin-enterprise/enterprise/src/web/enterprise/parameters/components/ParameterInputForm.jsx`
  const { colors } = useTheme();
  const Label = StyledControlLabel(colors.primary.tre);

  return (
    <Label {...props}>{children}</Label>
  );
};

const propTypes = {
  children: PropTypes.any.isRequired,
};

ControlLabel.propTypes = propTypes;
ControlLabelRaw.propTypes = propTypes;

export { ControlLabelRaw };
export default ControlLabel;
