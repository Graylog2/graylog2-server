// eslint-disable-next-line no-restricted-imports
import { Well as BootstrapWell } from 'react-bootstrap';
import styled, { css } from 'styled-components';

const Well = styled(BootstrapWell)(({ theme }) => css`
  background-color: ${theme.color.secondary.due};
  border-color: ${theme.color.secondary.tre};
`);

/** @component */
export default Well;
