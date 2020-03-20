import React, { createContext, useEffect, useState } from 'react';
import PropTypes from 'prop-types';

import CombinedProvider from 'injection/CombinedProvider';

const { ConfigurationsActions } = CombinedProvider.get('Configurations');

const DEFAULT_UI_CONTEXT = {};
const CUSTOMIZATION_CONFIG = 'org.graylog2.configuration.Customization';

export const CustomUiContext = createContext(DEFAULT_UI_CONTEXT);

const CustomUiProvider = ({ children }) => {
  const [badgeConfig, setBadgeConfig] = useState(DEFAULT_UI_CONTEXT);

  useEffect(() => {
    ConfigurationsActions.list(CUSTOMIZATION_CONFIG).then((configs) => {
      setBadgeConfig(configs);
    });
  }, []);

  const badgeUpdate = (newConfig) => {
    setBadgeConfig(newConfig);
  };

  return (
    <CustomUiContext.Provider value={{ badgeConfig, badgeUpdate }}>
      {children}
    </CustomUiContext.Provider>
  );
};

CustomUiProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default CustomUiProvider;
