import { memo } from 'react';
// eslint-disable-next-line no-restricted-imports
import { FormControl as BootstrapFormControl } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import { lighten, transparentize } from 'polished';

import teinte from 'theme/teinte';

const FormControl = memo(styled(BootstrapFormControl)(() => css`
  color: ${teinte.primary.tre};
  background-color: ${teinte.primary.due};
  border-color: ${teinte.secondary.tre};

  &:focus {
    border-color: ${teinte.tertiary.due};
    box-shadow: inset 0 1px 1px rgba(0, 0, 0, .075),
                0 0 8px ${transparentize(0.6, teinte.tertiary.due)};
  }

  &::-moz-placeholder,
  &:-ms-input-placeholder,
  &::-webkit-input-placeholder {
    color: ${lighten(0.6, teinte.primary.tre)};
  }

  &[disabled],
  &[readonly],
  fieldset[disabled] & {
    background-color: ${teinte.secondary.tre};
  }

  ~ .form-control-feedback.glyphicon {
    display: none;
  }
`));

FormControl.Static = BootstrapFormControl.Static;
FormControl.Feedback = BootstrapFormControl.Feedback;

/** @component */
export default FormControl;
