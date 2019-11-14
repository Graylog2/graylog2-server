// eslint-disable-next-line no-restricted-imports
import { InputGroup as BootstrapInputGroup } from 'react-bootstrap';
import styled from 'styled-components';

import { color } from 'theme';

const InputGroup = styled(BootstrapInputGroup)`
  .input-group-addon {
    color: ${color.gray[30]};
    background-color: ${color.gray[100]};
    border-color: ${color.gray[80]};
  }
`;

export default InputGroup;
