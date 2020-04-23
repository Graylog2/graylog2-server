// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { useStore } from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import CustomizationContext from './CustomizationContext';

const { CustomizationsStore } = CombinedProvider.get('Customizations');

const CustomizationProvider = ({ children }: { children: React.Node }) => {
  const customization = useStore(CustomizationsStore, (state) => state.customization);

  return customization
    ? (
      <CustomizationContext.Provider value={customization}>
        {children}
      </CustomizationContext.Provider>
    )
    : children;
};

CustomizationProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default CustomizationProvider;
