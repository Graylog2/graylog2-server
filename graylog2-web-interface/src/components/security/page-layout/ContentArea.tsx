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
import styled, { css } from 'styled-components';

const ContentArea = styled.div<{ $isSecurityPerspective?: boolean; }>(({ $isSecurityPerspective, theme }) => css`
  display: flex;
  flex-direction: column;
  padding-left: 0;
  padding-top: ${$isSecurityPerspective ? '0px' : '15px'};
  transition: all 0.33s ease-in-out;
  position: relative;
  width: 100%;
  height: 100%;
  overflow-y: auto;
  z-index: 10;
  top: 0;
  background: ${theme.colors.global.background};
`);

export default ContentArea;
