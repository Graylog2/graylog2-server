import { memo } from 'react';
import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { Jumbotron as BootstrapJumbotron } from 'react-bootstrap';

const Jumbotron = memo(styled(BootstrapJumbotron)(({ theme }) => css`
  color: ${theme.color.global.textDefault};
  background-color: ${theme.color.gray[100]};
`));

export default Jumbotron;
