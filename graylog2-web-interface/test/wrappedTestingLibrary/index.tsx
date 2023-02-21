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
import type { RenderOptions } from '@testing-library/react';
import { render } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import '@testing-library/jest-dom';
import 'jest-styled-components';
import type { RenderHookResult, RenderHookOptions } from '@testing-library/react-hooks';
import { renderHook, act as renderHookAct } from '@testing-library/react-hooks';
import type { QueryClientConfig } from '@tanstack/react-query';

import DefaultQueryClientProvider from '../DefaultQueryClientProvider';
import WrappingContainer from '../WrappingContainer';

export const renderWithWrapper = (Component: React.ReactElement<any>, options?: RenderOptions) => render(Component, {
  wrapper: WrappingContainer,
  ...options,
});

const renderHookWithWrapper = <TProps, TResult>(
  callback: (props: TProps) => TResult,
  options: RenderHookOptions<TProps> & { queryClientOptions?: QueryClientConfig } = {},
): RenderHookResult<TProps, TResult> => renderHook(
    callback,
    {
      ...options,
      wrapper: ({ children }) => {
        const CustomWrapper = options.wrapper as React.ElementType ?? React.Fragment;

        return (
          <DefaultQueryClientProvider options={options.queryClientOptions}>
            <CustomWrapper>
              {children}
            </CustomWrapper>
          </DefaultQueryClientProvider>
        );
      },
    });

export function asElement<T extends new(...args: any) => any> (elem: any, elementType: T): InstanceType<T> {
  if (elem && elem instanceof elementType) {
    // @ts-ignore
    return elem as T;
  }

  const { name } = elementType;
  throw new Error(`Unable to cast ${elem?.constructor?.name ?? 'unknown'} to ${name}!`);
}

export * from '@testing-library/react';
export {
  renderHookAct,
  renderWithWrapper as render,
  renderHookWithWrapper as renderHook,
};
