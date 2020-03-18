// eslint-disable-next-line no-restricted-imports
import { Well as BootstrapWell } from 'react-bootstrap';
import styled, { css } from 'styled-components';

const Well = styled(BootstrapWell)(({ theme }) => css`
  background-color: ${theme.color.gray[90]};
  border-color: ${theme.color.gray[80]};
`);

/** @component */
export default Well;
