// eslint-disable-next-line no-restricted-imports
import { InputGroup as BootstrapInputGroup } from 'react-bootstrap';
import styled from 'styled-components';
import { lighten } from 'polished';

import teinte from 'theme/teinte';

const InputGroup = styled(BootstrapInputGroup)`
  color: ${lighten(0.30, teinte.primary.tre)};
  background-color: ${teinte.secondary.due};
  border-color: ${teinte.secondary.tre};
`;

export default InputGroup;
