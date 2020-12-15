/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import PropTypes from 'prop-types';

import { useStore } from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import { CustomizationsStoreState } from 'stores/customizations/CustomizationsStore';
import { Store } from 'stores/StoreTypes';

import CustomizationContext from './CustomizationContext';

const { CustomizationsStore } = CombinedProvider.get('Customizations');

const CustomizationProvider = ({ children }) => {
  const customization = useStore(CustomizationsStore as Store<CustomizationsStoreState>, (state) => state.customization);

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
