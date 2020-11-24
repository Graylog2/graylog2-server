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
// @flow strict
import styled, { css } from 'styled-components';
import type { StyledComponent } from 'styled-components';

const ColorPreview: StyledComponent<{ color: string }, void, HTMLDivElement> = styled.div(({ color }) => css`
  height: 2rem;
  width: 2rem;
  margin-right: 0.4rem;

  background-color: ${color};
  border-radius: 4px;
  border: 1px solid rgba(0, 126, 255, 0.24);
`);

export default ColorPreview;
