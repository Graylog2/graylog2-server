// eslint-disable-next-line no-restricted-imports
import { Well as BootstrapWell } from 'react-bootstrap';
import styled, { css } from 'styled-components';

const Well = styled(BootstrapWell)(({ theme }) => css`
  background-color: ${theme.colors.variant.lightest.default};
  border-color: ${theme.colors.variant.light.default};
  color: ${theme.colors.variant.darker.default};
`);

/** @component */
export default Well;
