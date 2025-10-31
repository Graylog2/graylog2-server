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
import '@testing-library/jest-dom/jest-globals';
import '@testing-library/jest-dom';
import 'jest-styled-components';
import type { RenderHookOptions } from '@testing-library/react';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { QueryClientConfig } from '@tanstack/react-query';

import DefaultQueryClientProvider from '../DefaultQueryClientProvider';

const renderHookWithWrapper = <TProps, TResult>(
  callback: (props: TProps) => TResult,
  options: RenderHookOptions<TProps> & { queryClientOptions?: QueryClientConfig } = {},
) =>
  renderHook(callback, {
    ...options,

    wrapper: ({ children }: React.PropsWithChildren<{}>) => {
      const CustomWrapper = (options.wrapper as React.ElementType) ?? React.Fragment;

      return (
        <DefaultQueryClientProvider options={options.queryClientOptions}>
          <CustomWrapper>{children}</CustomWrapper>
        </DefaultQueryClientProvider>
      );
    },
  });

export { renderHookWithWrapper as renderHook, act, waitFor };
