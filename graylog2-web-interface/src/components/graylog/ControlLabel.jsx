import styled from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { ControlLabel as BootstrapControlLabel } from 'react-bootstrap';

import { color } from 'theme';

const ControlLabel = styled(BootstrapControlLabel)`
  color: ${color.global.textDefault};
`;

export default ControlLabel;
