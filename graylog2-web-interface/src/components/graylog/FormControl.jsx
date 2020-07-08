// eslint-disable-next-line no-restricted-imports
import { FormControl as BootstrapFormControl } from 'react-bootstrap';
import styled, { css } from 'styled-components';

const FormControl = styled(BootstrapFormControl)(({ theme }) => css`
  &.form-control {
    color: ${theme.colors.input.color};
    background-color: ${theme.colors.input.background};
    border-color: ${theme.colors.input.border};

    &::placeholder {
      color: ${theme.colors.input.placeholder};
    }

    &:focus {
      border-color: ${theme.colors.input.borderFocus};
      box-shadow: ${theme.colors.input.boxShadow};
    }

    &[disabled],
    &[readonly],
    fieldset[disabled] & {
      background-color: ${theme.colors.input.backgroundDisabled};
      color: ${theme.colors.input.colorDisabled};
    }

    ~ .form-control-feedback.glyphicon {
      display: none;
    }
  }
`);

FormControl.Static = BootstrapFormControl.Static;
FormControl.Feedback = BootstrapFormControl.Feedback;

/** @component */
export default FormControl;
