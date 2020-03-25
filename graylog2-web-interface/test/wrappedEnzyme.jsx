import { configure, mount, shallow } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import WrappingContainer from './WrappingContainer';

configure({ adapter: new Adapter() });

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
};
