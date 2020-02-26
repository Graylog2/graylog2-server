// eslint-disable-next-line no-restricted-imports
import { HelpBlock as BootstrapHelpBlock } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import { lighten } from 'polished';

const HelpBlock = styled(BootstrapHelpBlock)(({ theme }) => css`
  display: block;
  margin-top: 5px;
  margin-bottom: 10px;
  color: ${lighten(0.50, theme.color.primary.tre)};
`);

export default HelpBlock;
