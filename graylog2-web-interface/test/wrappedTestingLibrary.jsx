// @flow strict
import { type Element } from 'react';
import { render, type RenderOptionsWithoutCustomQueries } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import '@testing-library/jest-dom';

import WrappingContainer from './WrappingContainer';

export const renderWithWrapper = (Component: Element<any>, options: ?RenderOptionsWithoutCustomQueries) => render(Component, {
  wrapper: WrappingContainer,
  ...options,
});

export function asElement<T>(elem: HTMLElement, elementType: Class<T>): T {
  if (elem instanceof elementType) {
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
