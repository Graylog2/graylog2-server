import styled from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { ControlLabel as BootstrapControlLabel } from 'react-bootstrap';

const ControlLabel = styled(BootstrapControlLabel)(props => `
  color: ${props.theme.color.global.textDefault};
`);

export default ControlLabel;
