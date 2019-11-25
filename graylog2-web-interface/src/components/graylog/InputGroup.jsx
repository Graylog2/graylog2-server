import { memo } from 'react';
// eslint-disable-next-line no-restricted-imports
import { InputGroup as BootstrapInputGroup } from 'react-bootstrap';
import styled, { css } from 'styled-components';

const InputGroup = memo(styled(BootstrapInputGroup)(({ theme }) => css`
  .input-group-addon {
    color: ${theme.color.gray[30]};
    background-color: ${theme.color.gray[100]};
    border-color: ${theme.color.gray[80]};
  }
`));

InputGroup.Addon = memo(BootstrapInputGroup.Addon);
InputGroup.Button = memo(BootstrapInputGroup.Button);

export default InputGroup;
