import React from 'react';
import PropTypes from 'prop-types';
import { ThemeConsumer } from 'styled-components';

import { THEME_MODE_DARK, THEME_MODE_LIGHT } from 'theme/constants';

import StyleguideWrapper from './StyleguideWrapper';

const LogoRenderer = ({ children }) => {
  return (
    <StyleguideWrapper>
      <ThemeConsumer>
        {(theme) => {
          return (
            <>
              <h1>{children}</h1>
              <label>
                <input type="checkbox"
                       onChange={(evt) => {
                         theme.changeMode(evt.target.checked ? THEME_MODE_DARK : THEME_MODE_LIGHT);
                       }}
                       checked={theme.mode === THEME_MODE_DARK} />
                Enable Dark Mode
              </label>
            </>
          );
        }}
      </ThemeConsumer>
    </StyleguideWrapper>
  );
};

LogoRenderer.propTypes = {
  children: PropTypes.node.isRequired,
};

export default LogoRenderer;
