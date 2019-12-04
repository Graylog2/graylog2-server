import { render } from '@testing-library/react';

import WrappingContainer from './wrappingContainer';

export const renderWithWrapper = (Component, options = {}) => render(Component, {
  wrapper: WrappingContainer,
  ...options,
});

export * from '@testing-library/react';
export {
  renderWithWrapper as render,
}
