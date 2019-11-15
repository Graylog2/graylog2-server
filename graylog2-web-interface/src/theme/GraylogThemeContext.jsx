import React, { createContext, useContext } from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';
import { mix } from 'polished';

// TODO: make `teinte` a dynamic set of colors
import teinte from './teinte';

const defaultValues = { colors: teinte };
const ThemeColor = createContext(defaultValues);

function useTheme() {
  return useContext(ThemeColor);
}

function colorLevel(colorHex, level = 0) {
  /**
   * Recreating `color-level` from Bootstrap's SCSS functions
   * https://github.com/twbs/bootstrap/blob/08ba61e276a6393e8e2b97d56d2feb70a24fe22c/scss/_functions.scss#L97
   *
   * @param {string} color - any string that represents a color (ex: "#f00" or "rgb(255, 0, 0)")
   * @param {number} level - any positive or negative number (negative is brighter)
   */
  const colorBase = level > 0 ? teinte.primary.tre : teinte.primary.due;
  const absLevel = Math.abs(level) * 0.08; // TODO: make 8% a theme variable

  return mix(absLevel, colorBase, colorHex);
}

const GraylogThemeProvider = ({ children }) => {
  return (
    <ThemeColor.Provider value={defaultValues}>
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
export { useTheme, colorLevel };
