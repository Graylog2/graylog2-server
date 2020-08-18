import React, { useState } from 'react';
import PropTypes from 'prop-types';

import { PREFERENCES_THEME_MODE, THEME_MODE_DARK, THEME_MODE_LIGHT } from 'theme/constants';
import Store from 'logic/local-storage/Store';

const LOCAL_STORE_NAME = 'styleguide-theme-mode';
const currentMode = (Store.get(LOCAL_STORE_NAME) || PREFERENCES_THEME_MODE) || null;

const LogoRenderer = ({ children }) => {
  const [mode, setMode] = useState(currentMode);

  const handleChange = (evt) => {
    const nextMode = evt.target.checked ? THEME_MODE_DARK : THEME_MODE_LIGHT;

    Store.set(LOCAL_STORE_NAME, nextMode);

    setMode(nextMode);
  };

  return (
    <>
      <h1>{children}</h1>
      <label>
        <input type="checkbox" onChange={handleChange} checked={mode === THEME_MODE_DARK} /> Enable Dark Mode
      </label>
    </>
  );
};

LogoRenderer.propTypes = {
  children: PropTypes.node.isRequired,
};

export default LogoRenderer;
