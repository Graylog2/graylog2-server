import React, { memo } from 'react';
import PropTypes from 'prop-types';
// eslint-disable-next-line no-restricted-imports
import { FormGroup as BootstrapFormGroup } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import styledTheme from 'styled-theming';
import { darken, lighten } from 'polished';

import { themeModes, util } from 'theme';
import FormControl from './FormControl';
import { StyledAddon } from './InputGroup';

const VALID_STATES = ['error', 'warning', 'success'];

const createCss = validationState => css(({ theme }) => {
  console.log('validState', validationState);
  const variant = validationState === 'error' ? 'danger' : validationState;
  const text = util.colorLevel(theme.color.variant[variant], 6);
  const border = theme.color.variant[variant];
  const background = util.colorLevel(theme.color.variant[variant], -6);

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
          border-color: ${darken(0.10, border)};
          box-shadow: inset 0 1px 1px rgba(0, 0, 0, 0.075), 0 0 6px ${lighten(0.20, border)};
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

const validationStates = {};
VALID_STATES.forEach((validState) => {
  const colorModes = {};

  themeModes.forEach((mode) => {
    colorModes[mode] = createCss(validState);
  });

  validationStates[validState] = colorModes;
});

const validationStyleVariants = styledTheme.variants('mode', 'validationState', validationStates);

const StyledFormGroup = styled(BootstrapFormGroup)`
  ${validationStyleVariants};
`;

const FormGroup = memo(({ children, validationState, ...props }) => {
  return (
    <StyledFormGroup validationState={validationState} {...props}>
      {children}
    </StyledFormGroup>
  );
});

FormGroup.propTypes = {
  children: PropTypes.node.isRequired,
  validationState: PropTypes.oneOf([null, ...VALID_STATES]),
};

FormGroup.defaultProps = {
  validationState: null,
};

export default FormGroup;
