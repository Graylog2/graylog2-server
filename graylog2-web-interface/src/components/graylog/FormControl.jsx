// eslint-disable-next-line no-restricted-imports
import { FormControl as BootstrapFormControl } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import { transparentize } from 'polished';

const FormControl = styled(BootstrapFormControl)(({ theme }) => {
  return css`
    color: ${theme.color.global.textDefault};
    background-color: ${theme.color.gray[100]};
    border-color: ${theme.color.gray[80]};

    &::placeholder {
      color: ${theme.color.gray[60]};
    }

    &:focus {
      border-color: ${theme.color.variant.light.info};
      box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.075),
        0 0 8px ${transparentize(0.6, theme.color.variant.light.info)};
    }

    &[disabled],
    &[readonly],
    fieldset[disabled] & {
      background-color: ${theme.color.gray[80]};
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
