// eslint-disable-next-line no-restricted-imports
import { Well as BootstrapWell } from 'react-bootstrap';
import styled from 'styled-components';

import teinte from 'theme/teinte';

const Well = styled(BootstrapWell)`
  background-color: ${teinte.secondary.due};
  border-color: ${teinte.secondary.tre};
`;

/** @component */
export default Well;
