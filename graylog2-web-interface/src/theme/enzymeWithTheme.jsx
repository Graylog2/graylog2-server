import React from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';
import { mount, shallow } from 'enzyme';

const GraylogThemeProvider = ({ children }) => {
  return (
    <ThemeProvider theme={{ mode: 'teinte' }}>
      {children}
    </ThemeProvider>
  );
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.node.isRequired,
}

export const shallowWithTheme = (Component, options = {}) => shallow(Component, {
  wrappingComponent: GraylogThemeProvider,
  ...options,
});

export const mountWithTheme = (Component, options = {}) => mount(Component, {
  wrappingComponent: GraylogThemeProvider,
  ...options,
});
