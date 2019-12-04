import { mount, shallow } from 'enzyme';

import WrappingContainer from './wrappingContainer';

export const shallowWithWrapper = (Component, options = {}) => shallow(Component, {
  wrappingComponent: WrappingContainer,
  ...options,
});

export const mountWithWrapper = (Component, options = {}) => mount(Component, {
  wrappingComponent: WrappingContainer,
  ...options,
});

export * from 'enzyme';
export {
  mountWithWrapper as mount,
  shallowWithWrapper as shallow,
}
