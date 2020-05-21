// eslint-disable-next-line no-restricted-imports
import { FormControl as BootstrapFormControl } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

const FormControl = styled(BootstrapFormControl)(({ theme }) => {
  return css`
    color: ${theme.colors.global.textDefault};
    background-color: ${theme.colors.global.contentBackground};
    border-color: ${theme.colors.gray[80]};

    &::placeholder {
      color: ${theme.colors.gray[60]};
    }

    &:focus {
      border-color: ${theme.colors.variant.light.info};
      box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.075),
        0 0 8px ${chroma(theme.colors.variant.light.info).alpha(0.4).css()};
    }

    &[disabled],
    &[readonly],
    fieldset[disabled] & {
      background-color: ${theme.colors.gray[80]};
    }

    ~ .form-control-feedback.glyphicon {
      display: none;
    }
  `;
});

FormControl.Static = BootstrapFormControl.Static;
FormControl.Feedback = BootstrapFormControl.Feedback;

/** @component */
export default FormControl;
