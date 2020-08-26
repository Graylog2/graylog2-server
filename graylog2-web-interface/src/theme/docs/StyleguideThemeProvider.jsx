import React, { useEffect, useMemo, useState } from 'react';
import PropTypes from 'prop-types';
import { createGlobalStyle, css, ThemeProvider } from 'styled-components';
import noop from 'lodash/noop';

import buttonStyles from 'components/graylog/styles/buttonStyles';
import aceEditorStyles from 'components/graylog/styles/aceEditorStyles';
import { breakpoints, colors, fonts, utils } from 'theme/index';
import { DEFAULT_THEME_MODE, THEME_MODE_LIGHT } from 'theme/constants';
import Store from 'logic/local-storage/Store';

const LOCAL_STORE_NAME = 'styleguide-theme-mode';
const GlobalThemeStyles = createGlobalStyle(({ theme }) => css`
  html {
    font-size: ${theme.fonts.size.root} !important; /* override Bootstrap default */
  }
  
  body {
    background-color: ${theme.colors.global.background};
    color: ${theme.colors.global.textDefault};
    font-family: ${theme.fonts.family.body};
    overflow-x: hidden;
    margin-top: 50px;
    min-height: calc(100vh - 50px);
  }
  
  ul {
    list-style-type: none;
    margin: 0;
  }

  hr {
    border-top: 1px solid ${theme.colors.global.background};
  }

  h1,
  h2,
  h3,
  h4,
  h5,
  h6 {
    font-weight: normal;
    padding: 0;
    margin: 0;
    color: ${theme.colors.global.textDefault};
  }

  h1 {
    font-size: ${theme.fonts.size.h1};
  }

  h2 {
    font-size: ${theme.fonts.size.h2};
  }

  h3 {
    font-size: ${theme.fonts.size.h3};
  }

  h4 {
    font-size: ${theme.fonts.size.h4};
  }

  h5 {
    font-size: ${theme.fonts.size.h5};
  }

  h6 {
    font-size: ${theme.fonts.size.h6};
    font-weight: bold;
  }

  a {
    color: ${theme.colors.global.link} !important;
    
    :hover {
      color: ${theme.colors.global.linkHover} !important;
    }
  }

  /* Remove boostrap outline */
  a:active,
  select:active,
  input[type="file"]:active,
  input[type="radio"]:active,
  input[type="checkbox"]:active,
  .btn:active {
    outline: none;
    outline-offset: 0;
  }

  input.form-control,
  select.form-control,
  textarea.form-control {
    color: ${theme.colors.input.color};
    background-color: ${theme.colors.input.background};
    border-color: ${theme.colors.input.border};
    font-family: ${theme.fonts.family.body};

    &::placeholder {
      color: ${theme.colors.input.placeholder};
    }

    &:focus {
      border-color: ${theme.colors.input.borderFocus};
      box-shadow: ${theme.colors.input.boxShadow};
    }
    
    &[disabled],
    &[readonly],
    fieldset[disabled] & {
      background-color: ${theme.colors.input.backgroundDisabled};
      color: ${theme.colors.input.colorDisabled};
    }
  }

  label {
    font-size: ${theme.fonts.size.large};
  }

  legend small {
    color: ${theme.colors.gray[60]};
    margin-left: 5px;
  }

  small {
    font-size: ${theme.fonts.size.small};
  }
`);

const StyleguideThemeProvider = ({ children, defaultMode }) => {
  const [mode, setMode] = useState(defaultMode);
  const [themeColors, setThemeColors] = useState(colors[defaultMode]);

  const changeMode = (nextMode) => {
    if (colors[nextMode]) {
      Store.set(LOCAL_STORE_NAME, nextMode);
      setMode(nextMode);
      setThemeColors(colors[nextMode]);

      return { [LOCAL_STORE_NAME]: nextMode };
    }

    return null;
  };

  const formattedUtils = {
    ...utils,
    colorLevel: themeColors ? utils.colorLevel(themeColors) : noop,
    readableColor: themeColors ? utils.readableColor(themeColors) : noop,
  };

  const themeMemo = useMemo(() => ({
    mode,
    changeMode,
    breakpoints,
    colors: themeColors,
    fonts,
    components: {
      button: buttonStyles({ colors: themeColors, utils: formattedUtils }),
      aceEditor: aceEditorStyles({ colors: themeColors }),
    },
    utils: formattedUtils,
  }), [mode, themeColors]);

  const [theme, setTheme] = useState(themeMemo);

  useEffect(() => {
    if (themeColors) {
      setTheme(themeMemo);
    }
  }, [themeColors]);

  return theme ? (
    <ThemeProvider theme={theme} key={theme?.mode}>
      <>
        <GlobalThemeStyles />
        {children}
      </>
    </ThemeProvider>
  ) : null;
};

StyleguideThemeProvider.propTypes = {
  children: PropTypes.node.isRequired,
  defaultMode: PropTypes.string,
};

StyleguideThemeProvider.defaultProps = {
  defaultMode: Store.get(LOCAL_STORE_NAME) || DEFAULT_THEME_MODE || THEME_MODE_LIGHT,
};

export default StyleguideThemeProvider;
