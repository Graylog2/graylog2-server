import styled from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { ControlLabel as BootstrapControlLabel } from 'react-bootstrap';

import teinte from 'theme/teinte';

const ControlLabel = styled(BootstrapControlLabel)`
  color: ${teinte.primary.tre};
  font-weight: bold;
  margin-bottom: 5px;
  display: inline-block;
`;

/** @component */
export default ControlLabel;
