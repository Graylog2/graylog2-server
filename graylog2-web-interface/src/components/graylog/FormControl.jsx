import styled, { css } from 'styled-components';
// eslint-disable-next-line no-restricted-imports
import { FormControl as BootstrapFormControl } from 'react-bootstrap';
import { darken, lighten, transparentize } from 'polished';

import teinte from 'theme/teinte';
import { colorLevel } from 'theme/GraylogThemeContext';
// NOTE: Needs to be global style
const validationStyles = (textColor, borderColor, backgroundColor) => css`
  .help-block,
  .control-label,
  .radio,
  .checkbox,
  .radio-inline,
  .checkbox-inline,
  &.radio label,
  &.checkbox label,
  &.radio-inline label,
  &.checkbox-inline label  {
    color: ${textColor};
  }

  .form-control {
    border-color: ${borderColor};

    &:focus {
      border-color: ${darken(0.10, borderColor)};
      box-shadow(inset 0 1px 1px rgba(0, 0, 0, .075), 0 0 6px ${lighten(0.20, borderColor)});
    }
  }

  .input-group-addon {
    color: ${textColor};
    background-color: ${backgroundColor};
    border-color: ${borderColor};
  }

  .form-control-feedback {
    color: ${textColor};
  }
`;

const FormControl = styled(BootstrapFormControl)`
  color: ${teinte.primary.tre};
  background-color: ${teinte.primary.due};
  background-image: none;
  border: 1px solid ${teinte.secondary.tre};

  &:focus {
    border-color: ${teinte.tertiary.due};
    box-shadow(inset 0 1px 1px rgba(0, 0, 0, .075), 0 0 8px ${transparentize(0.6, teinte.tertiary.due)});
  }

  &::-moz-placeholder { color: ${lighten(0.6, teinte.primary.tre)}; }
  &:-ms-input-placeholder { color: ${lighten(0.6, teinte.primary.tre)}; }
  &::-webkit-input-placeholder  { color: ${lighten(0.6, teinte.primary.tre)}; }

  &[disabled],
  &[readonly],
  fieldset[disabled] & {
    background-color: ${teinte.secondary.tre};
  }

  &.has-success {
    ${validationStyles(teinte.tertiary.tre, teinte.tertiary.tre, colorLevel(teinte.tertiary.tre, -6))}
  }



  ~ .help-block {
    color: ${lighten(0.25, teinte.primary.tre)};
  }

  ~ .form-control-feedback.glyphicon { display: none; }
`;
  /* .has-warning {
    ${validationStyles(@state-warning-text; @state-warning-text; @state-warning-bg);}
  }

  .has-error {
    ${alidationStyles(@state-danger-text; @state-danger-text; @state-danger-bg);}
  } */
export default FormControl;
