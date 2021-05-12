/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React, { memo } from 'react';
import PropTypes from 'prop-types';
// eslint-disable-next-line no-restricted-imports
import { FormGroup as BootstrapFormGroup } from 'react-bootstrap';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

import { CONTROL_CLASS as COMMON_SELECT_CONTROL_CLASS } from 'components/common/Select.tsx';

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

      ${FormControl}, .${COMMON_SELECT_CONTROL_CLASS} {
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

      .${COMMON_SELECT_CONTROL_CLASS} {
        border-top-left-radius: 0;
        border-bottom-left-radius: 0;
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
