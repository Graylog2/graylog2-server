// eslint-disable-next-line no-restricted-imports
import { HelpBlock as BootstrapHelpBlock } from 'react-bootstrap';
import styled from 'styled-components';

import { color } from 'theme';

const HelpBlock = styled(BootstrapHelpBlock)`
  display: block;
  margin-top: 5px;
  margin-bottom: 10px;
  color: ${color.gray[50]};
`;

export default HelpBlock;
