import React from 'react';
import PropTypes from 'prop-types';
// Import default implementation from react-styleguidist using the full path
import DefaultStyleGuideRenderer from 'react-styleguidist/lib/client/rsg-components/StyleGuide/StyleGuideRenderer';
import styled, { ThemeConsumer } from 'styled-components';

import StyleguideThemeProvider from 'theme/docs/StyleguideThemeProvider';
import { THEME_MODE_DARK, THEME_MODE_LIGHT } from 'theme/constants';

const Toggle = styled.div`
  float: right;
  background-color: white;
  border: 1px solid black;
  padding: 4px 6px;
  position: sticky;
  top: 0;
  z-index: 2;
  color: black;
  
  + div {
    clear: right;
  }
`;

export function StyleGuideRenderer({ children, ...rest }) {
  const handleModeChange = (changeMode, mode) => {
    changeMode(mode === THEME_MODE_LIGHT ? THEME_MODE_DARK : THEME_MODE_LIGHT);
    window.location.reload();
  };

  return (
    <StyleguideThemeProvider>
      <ThemeConsumer>
        {(theme) => {
          return (
            <div>
              <Toggle>
                <button type="button" onClick={() => handleModeChange(theme.changeMode, theme.mode)}>
                  Toggle Theme Mode
                </button>
              </Toggle>

              <DefaultStyleGuideRenderer {...rest}>
                <div className="clearfix">
                  {children}
                </div>
              </DefaultStyleGuideRenderer>
            </div>
          );
        }}
      </ThemeConsumer>
    </StyleguideThemeProvider>
  );
}

StyleGuideRenderer.propTypes = {
  children: PropTypes.node.isRequired,
};

export default StyleGuideRenderer;
