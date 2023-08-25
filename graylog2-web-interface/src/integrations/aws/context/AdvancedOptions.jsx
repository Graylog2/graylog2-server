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
import React, { createContext, useState } from 'react';
import PropTypes from 'prop-types';

export const AdvancedOptionsContext = createContext();

export const AdvancedOptionsProvider = ({ children }) => {
  const [isAdvancedOptionsVisible, setAdvancedOptionsVisibility] = useState(false);
  const [isAWSCustomEndpointsVisible, setAWSCustomEndpointsVisibility] = useState(false);

  return (
    <AdvancedOptionsContext.Provider value={{
      isAdvancedOptionsVisible,
      isAWSCustomEndpointsVisible,
      setAdvancedOptionsVisibility,
      setAWSCustomEndpointsVisibility,
    }}>
      {children}
    </AdvancedOptionsContext.Provider>
  );
};

AdvancedOptionsProvider.propTypes = {
  children: PropTypes.any.isRequired,
};
