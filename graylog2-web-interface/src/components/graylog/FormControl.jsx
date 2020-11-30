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
// eslint-disable-next-line no-restricted-imports
import { FormControl as BootstrapFormControl } from 'react-bootstrap';
import styled, { css, DefaultTheme } from 'styled-components';

const FormControl = styled(BootstrapFormControl)(({ theme }: { theme: DefaultTheme }) => css`
  &.form-control:not([type="range"]) {
    color: ${theme.colors.input.color};
    background-color: ${theme.colors.input.background};
    border-color: ${theme.colors.input.border};

    &::placeholder {
      color: ${theme.colors.input.placeholder};
    }

    &:focus {
      border-color: ${theme.colors.input.borderFocus};
      box-shadow: ${theme.colors.input.boxShadow};
    }

    &[disabled],
    &[readonly],
    fieldset[disabled] & {
      background-color: ${theme.colors.input.backgroundDisabled};
      color: ${theme.colors.input.colorDisabled};
    }

    ~ .form-control-feedback.glyphicon {
      display: none;
    }
  }
`);

FormControl.Static = BootstrapFormControl.Static;
FormControl.Feedback = BootstrapFormControl.Feedback;

/** @component */
export default FormControl;
