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
import { TBreakpoint } from '../../styled-components/types';
import { ThemeBase } from '../../types';

const breakpoints = (baseBreakpoints: ThemeBase['breakpoints']) => ({
  min: Object.fromEntries(Object.entries(baseBreakpoints).map(([key, value]) => [key, `${value}px`])) as TBreakpoint,
  max: Object.fromEntries(
    Object.entries(baseBreakpoints).map(([key, value]) => [key, `${value - 1}px`]),
  ) as TBreakpoint,
  px: {
    max: Object.fromEntries(Object.entries(baseBreakpoints).map(([key, value]) => [key, value - 1])) as Record<
      keyof TBreakpoint,
      number
    >,
    min: baseBreakpoints,
  },
});

export default breakpoints;
