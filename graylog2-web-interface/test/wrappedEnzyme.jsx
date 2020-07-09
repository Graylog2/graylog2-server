// @flow strict
import { configure, mount, shallow, type ReactWrapper, type ShallowWrapper } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import 'jest-styled-components';

import WrappingContainer from './WrappingContainer';

configure({ adapter: new Adapter() });

export const shallowWithWrapper = <T>(Component: React$Element<T>, options: any = {}): ShallowWrapper<T> => shallow(Component, {
  wrappingComponent: WrappingContainer,
  ...options,
});

export const mountWithWrapper = <T>(Component: React$Element<T>, options: any = {}): ReactWrapper<T> => mount(Component, {
  wrappingComponent: WrappingContainer,
  ...options,
});

export * from 'enzyme';
export {
  mountWithWrapper as mount,
  shallowWithWrapper as shallow,
};
