import React, { createContext, useContext } from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';

// TODO: make `teinte` a dynamic set of colors
import teinte from './teinte';
import GlobalThemeStyles from './GlobalThemeStyles';

const defaultValues = { colors: teinte };
const ThemeColor = createContext(defaultValues);

function useTheme() {
  return useContext(ThemeColor);
}

const GraylogThemeProvider = ({ children }) => {
  return (
    <ThemeColor.Provider value={defaultValues}>
      <GlobalThemeStyles />
      {/* NOTE: mode can be `teinte` and will eventually need to come from User Preferences */}
      <ThemeProvider theme={{ mode: 'teinte' }}>
        {children}
      </ThemeProvider>
    </ThemeColor.Provider>
  );
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export default GraylogThemeProvider;
export { useTheme };
