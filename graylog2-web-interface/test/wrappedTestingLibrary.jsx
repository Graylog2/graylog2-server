import { render } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import '@testing-library/jest-dom';

import WrappingContainer from './WrappingContainer';

export const renderWithWrapper = (Component, options = {}) => render(Component, {
  wrapper: WrappingContainer,
  ...options,
});

export * from '@testing-library/react';
export {
  renderWithWrapper as render,
};
