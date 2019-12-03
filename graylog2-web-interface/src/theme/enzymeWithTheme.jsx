import { mount, shallow } from 'enzyme';

import GraylogThemeProvider from 'theme/GraylogThemeProvider';

export const shallowWithTheme = (Component, options = {}) => shallow(Component, {
  wrappingComponent: GraylogThemeProvider,
  ...options,
});

export const mountWithTheme = (Component, options = {}) => mount(Component, {
  wrappingComponent: GraylogThemeProvider,
  ...options,
});
