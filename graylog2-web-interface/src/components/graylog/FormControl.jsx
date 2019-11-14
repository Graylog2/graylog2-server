import { memo } from 'react';
// eslint-disable-next-line no-restricted-imports
import { FormControl as BootstrapFormControl } from 'react-bootstrap';
import styled from 'styled-components';
import { lighten, transparentize } from 'polished';

import { color } from 'theme';

const FormControl = memo(styled(BootstrapFormControl)`
  color: ${color.global.textDefault};
  background-color: ${color.gray[100]};
  border-color: ${color.gray[80]};

  &:focus {
    border-color: ${color.variant.light.info};
    box-shadow: inset 0 1px 1px rgba(0, 0, 0, .075), 0 0 8px ${transparentize(0.6, color.variant.light.info)};
  }

  &::-moz-placeholder,
  &:-ms-input-placeholder,
  &::-webkit-input-placeholder {
    color: ${lighten(0.6, color.primary.tre)};
  }

  &[disabled],
  &[readonly],
  fieldset[disabled] & {
    background-color: ${color.gray[80]};
  }

  ~ .form-control-feedback.glyphicon { display: none; }
`);

FormControl.Static = BootstrapFormControl.Static;
FormControl.Feedback = BootstrapFormControl.Feedback;

export default FormControl;
