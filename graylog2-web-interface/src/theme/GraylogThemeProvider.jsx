import React from 'react';
import PropTypes from 'prop-types';
import { ThemeProvider } from 'styled-components';

import { color } from 'theme';
import buttonStyles from 'components/graylog/styles/buttonStyles';

import 'opensans-npm-webfont/open_sans.css';
import 'opensans-npm-webfont/open_sans_italic.css';
import 'opensans-npm-webfont/open_sans_bold.css';

/* NOTE: mode will eventually need to come from User Preferences */
const THEME_MODE = 'teinte';

const GraylogThemeProvider = ({ children }) => {
  return (
    <ThemeProvider theme={{
      mode: THEME_MODE,
      color: color[THEME_MODE],
      components: {
        button: buttonStyles({ color: color[THEME_MODE] }),
      },
    }}>
      {children}
    </ThemeProvider>
  );
};

GraylogThemeProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export default GraylogThemeProvider;
