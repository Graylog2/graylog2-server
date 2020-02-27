import { memo } from 'react';
// eslint-disable-next-line no-restricted-imports
import { FormControl as BootstrapFormControl } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import { lighten, transparentize } from 'polished';

const FormControl = memo(styled(BootstrapFormControl)(({ theme }) => css`
  color: ${theme.color.primary.tre};
  background-color: ${theme.color.primary.due};
  border-color: ${theme.color.secondary.tre};

  &::placeholder {
    color: ${lighten(0.6, theme.color.primary.tre)};
  }

  &:focus {
    border-color: ${theme.color.tertiary.due};
    box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.075),
      0 0 8px ${transparentize(0.6, theme.color.tertiary.due)};
  }

  &[disabled],
  &[readonly],
  fieldset[disabled] & {
    background-color: ${theme.color.secondary.tre};
  }

  ~ .form-control-feedback.glyphicon {
    display: none;
  }
`));

FormControl.Static = BootstrapFormControl.Static;
FormControl.Feedback = BootstrapFormControl.Feedback;

/** @component */
export default FormControl;
