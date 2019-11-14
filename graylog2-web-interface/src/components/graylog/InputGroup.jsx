// eslint-disable-next-line no-restricted-imports
import { InputGroup as BootstrapInputGroup } from 'react-bootstrap';
import styled from 'styled-components';
import { lighten } from 'polished';

import { color } from 'theme';

const InputGroup = styled(BootstrapInputGroup)`
  .input-group-addon {
    color: ${lighten(0.30, color.primary.tre)};
    background-color: ${color.primary.due};
    border-color: ${color.secondary.tre};
  }
`;

export default InputGroup;
