// eslint-disable-next-line no-restricted-imports
import { Well as BootstrapWell } from 'react-bootstrap';
import styled from 'styled-components';

import { color } from 'theme';

const Well = styled(BootstrapWell)`
  background-color: ${color.secondary.due};
  border-color: ${color.secondary.tre};
`;

export default Well;
