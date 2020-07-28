import React, { memo } from 'react';
import PropTypes from 'prop-types';
// eslint-disable-next-line no-restricted-imports
import { FormGroup as BootstrapFormGroup } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

import FormControl from './FormControl';
import { StyledAddon } from './InputGroup';

const StyledFormGroup = styled(BootstrapFormGroup)(({ theme, validationState }) => {
  const variant = validationState === 'error' ? 'danger' : validationState;

  if (!variant) {
    return undefined;
  }

  const text = theme.colors.variant.dark[variant];
  const border = theme.colors.variant.lighter[variant];
  const background = theme.colors.variant.lightest[variant];

  return css`
    &.has-${validationState} {
      .help-block,
      .control-label,
      .radio,
      .checkbox,
      .radio-inline,
      .checkbox-inline,
      &.radio label,
      &.checkbox label,
      &.radio-inline label,
      &.checkbox-inline label {
        color: ${text};
      }

      ${FormControl} {
        border-color: ${border};

        &:focus {
          border-color: ${chroma(border).darken(0.1)};
          box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.075), 0 0 6px ${chroma(border).brighten(0.2)};
        }
      }

      ${StyledAddon} {
        color: ${text};
        background-color: ${background};
        border-color: ${border};
      }

      .form-control-feedback {
        color: ${text};
      }
    }
  `;
});

const FormGroup = memo(({ children, validationState, ...props }) => {
  return (
    <StyledFormGroup validationState={validationState} {...props}>
      {children}
    </StyledFormGroup>
  );
});

FormGroup.propTypes = {
  children: PropTypes.node.isRequired,
  validationState: PropTypes.oneOf([null, 'error', 'success', 'warning']),
};

FormGroup.defaultProps = {
  validationState: null,
};

export default FormGroup;
