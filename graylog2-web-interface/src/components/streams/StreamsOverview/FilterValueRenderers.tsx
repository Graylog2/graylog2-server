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
import * as React from 'react';
import styled, { css } from 'styled-components';

const StatusIndicator = styled.div<{ $disabled: 'true' | 'false' }>(({ $disabled, theme }) => css`
  display: inline-flex;
  border-radius: 50%;
  height: 10px;
  width: 10px;
  background-color: ${$disabled === 'true' ? theme.colors.variant.warning : theme.colors.variant.success};
  margin-right: 3px;
  border: 1px solid ${theme.colors.gray[70]};
`);

const FilterValueRenderers = {
  disabled: (value: 'true' | 'false', title: string) => (
    <>
      <StatusIndicator $disabled={value} />
      {title}
    </>
  ),
};

export default FilterValueRenderers;
