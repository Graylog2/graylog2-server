import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { ControlLabel as BootstrapControlLabel } from 'react-bootstrap';

const ControlLabel = styled(BootstrapControlLabel)(({ theme }) => css`
  color: ${theme.color.global.textDefault};
`);

export default ControlLabel;
