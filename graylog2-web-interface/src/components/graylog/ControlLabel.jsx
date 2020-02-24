import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { ControlLabel as BootstrapControlLabel } from 'react-bootstrap';

const ControlLabel = styled(BootstrapControlLabel)(({ theme }) => css`
  color: ${theme.color.primary.tre};
  font-weight: bold;
  margin-bottom: 5px;
  display: inline-block;
`);

export default ControlLabel;
