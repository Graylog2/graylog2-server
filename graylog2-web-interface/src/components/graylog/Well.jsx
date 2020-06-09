// eslint-disable-next-line no-restricted-imports
import { Well as BootstrapWell } from 'react-bootstrap';
import styled, { css } from 'styled-components';

const Well = styled(BootstrapWell)(({ theme }) => css`
  background-color: ${theme.colors.gray[90]};
  border-color: ${theme.colors.gray[80]};
`);

/** @component */
export default Well;
