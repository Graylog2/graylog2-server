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
import { type Element } from 'react';
import { render, type RenderOptionsWithoutCustomQueries } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import '@testing-library/jest-dom';
import 'jest-styled-components';

import WrappingContainer from './WrappingContainer';

export const renderWithWrapper = (Component: Element<any>, options: ?RenderOptionsWithoutCustomQueries) => render(Component, {
  wrapper: WrappingContainer,
  ...options,
});

export function asElement<T>(elem: ?(HTMLElement | Node), elementType: Class<T>): T {
  if (elem && elem instanceof elementType) {
    return (elem: T);
  }

  // $FlowFixMe: Why is it not possible to extract the name of the class?
  const { name } = elementType;
  throw new Error(`Unable to cast ${elem?.constructor?.name ?? 'unknown'} to ${name}!`);
}

export * from '@testing-library/react';
export {
  renderWithWrapper as render,
};
