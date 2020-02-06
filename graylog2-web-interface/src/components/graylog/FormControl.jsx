import { memo } from 'react';
// eslint-disable-next-line no-restricted-imports
import { FormControl as BootstrapFormControl } from 'react-bootstrap';
import styled from 'styled-components';
import { lighten, transparentize } from 'polished';

import teinte from 'theme/teinte';

const FormControl = memo(styled(BootstrapFormControl)`
  color: ${teinte.primary.tre};
  background-color: ${teinte.primary.due};
  border-color: ${teinte.secondary.tre};

  &::-moz-placeholder,
  &::-webkit-input-placeholder,
  &:-ms-input-placeholder {
    color: ${lighten(0.6, teinte.primary.tre)};
  }

  &:focus {
    border-color: ${teinte.tertiary.due};
    box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.075),
      0 0 8px ${transparentize(0.6, teinte.tertiary.due)};
  }

  &[disabled],
  &[readonly],
  fieldset[disabled] & {
    background-color: ${teinte.secondary.tre};
  }

  ~ .form-control-feedback.glyphicon { display: none; }
`);

FormControl.Static = BootstrapFormControl.Static;
FormControl.Feedback = BootstrapFormControl.Feedback;

export default FormControl;
