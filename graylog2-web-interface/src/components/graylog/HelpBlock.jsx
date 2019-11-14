// eslint-disable-next-line no-restricted-imports
import { HelpBlock as BootstrapHelpBlock } from 'react-bootstrap';
import styled from 'styled-components';
import { lighten } from 'polished';

import { color } from 'theme';

const HelpBlock = styled(BootstrapHelpBlock)`
  display: block;
  margin-top: 5px;
  margin-bottom: 10px;
  color: ${lighten(0.50, color.primary.tre)};
`;

export default HelpBlock;
