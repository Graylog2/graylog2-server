import styled from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { ControlLabel as BootstrapControlLabel } from 'react-bootstrap';

import { color } from 'theme';

const ControlLabel = styled(BootstrapControlLabel)`
  color: ${color.primary.tre};
  font-weight: bold;
  margin-bottom: 5px;
  display: inline-block;
`;

export default ControlLabel;
