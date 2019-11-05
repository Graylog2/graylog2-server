import React, { forwardRef } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { ControlLabel as BootstrapControlLabel } from 'react-bootstrap';

import teinte from 'theme/teinte';

const ControlLabelRaw = styled(BootstrapControlLabel)`
  color: ${teinte.primary.tre};
  font-weight: bold;
  margin-bottom: 5px;
  display: inline-block;
`;

const ControlLabel = forwardRef((props, ref) => <ControlLabelRaw ref={ref} {...props} />);

const propTypes = {
  children: PropTypes.any.isRequired,
};

ControlLabel.propTypes = propTypes;
ControlLabelRaw.propTypes = propTypes;

export default ControlLabel;
export { ControlLabelRaw };
