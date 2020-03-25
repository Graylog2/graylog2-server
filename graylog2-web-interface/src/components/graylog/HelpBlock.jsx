// eslint-disable-next-line no-restricted-imports
import { HelpBlock as BootstrapHelpBlock } from 'react-bootstrap';
import styled, { css } from 'styled-components';

const HelpBlock = styled(BootstrapHelpBlock)(({ theme }) => css`
  display: block;
  margin-top: 5px;
  margin-bottom: 10px;
  color: ${theme.color.gray[50]};
`);

/** @component */
export default HelpBlock;
