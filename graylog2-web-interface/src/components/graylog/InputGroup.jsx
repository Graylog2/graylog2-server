import { memo } from 'react';
// eslint-disable-next-line no-restricted-imports
import { InputGroup as BootstrapInputGroup } from 'react-bootstrap';
import styled from 'styled-components';
import { lighten } from 'polished';

import teinte from 'theme/teinte';

const InputGroup = memo(styled(BootstrapInputGroup)`
  .input-group-addon {
    color: ${lighten(0.30, teinte.primary.tre)};
    background-color: ${teinte.primary.due};
    border-color: ${teinte.secondary.tre};
  }
`);

InputGroup.Addon = memo(BootstrapInputGroup.Addon);
InputGroup.Button = memo(BootstrapInputGroup.Button);

/** @component */
export default InputGroup;
