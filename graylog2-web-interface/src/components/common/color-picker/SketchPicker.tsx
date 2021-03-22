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
import { SketchPicker } from 'react-color';
import styled, { css } from 'styled-components';

export default styled(SketchPicker)(({ theme }) => css`
  background-color: ${theme.colors.global.background} !important;
  box-shadow: ${theme.colors.global.navigationBoxShadow} 0px 0px 2px 1px, ${theme.colors.global.navigationBoxShadow} 0px 5px 5px !important;
  
  &::after {
    border-right-color: ${theme.colors.global.background} !important;
  }

  input {
    color: ${theme.colors.input.color} !important;
    background: ${theme.colors.input.background} !important;
    border-color: ${theme.colors.input.border} !important;
    
    + span {
      color: ${theme.colors.input.color} !important;
    }
  }
`);
