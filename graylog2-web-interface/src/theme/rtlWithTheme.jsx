import React from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';
import { render } from '@testing-library/react';

const GraylogThemeProvider = ({ children }) => {
  return (
    <ThemeProvider theme={{ mode: 'teinte' }}>
      {children}
    </ThemeProvider>
  );
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export const renderWithTheme = (Component, options = {}) => render(Component, {
  wrapper: GraylogThemeProvider,
  ...options,
});


export * from '@testing-library/react';
export { renderWithTheme as render };
