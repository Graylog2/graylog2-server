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

import Store from 'logic/local-storage/Store';

export const ScratchpadContext = createContext();

export const ScratchpadProvider = ({ children, loginName }) => {
  const localStorageItem = `gl-scratchpad-${loginName}`;
  const scratchpadStore = Store.get(localStorageItem) || {};
  const [isScratchpadVisible, setVisibility] = useState(scratchpadStore.opened || false);

  const toggleScratchpadVisibility = () => {
    const currentStorage = Store.get(localStorageItem);

    Store.set(localStorageItem, { ...currentStorage, opened: !isScratchpadVisible });
    setVisibility(!isScratchpadVisible);
  };

  const setScratchpadVisibility = (opened) => {
    const currentStorage = Store.get(localStorageItem);

    Store.set(localStorageItem, { ...currentStorage, opened });
    setVisibility(opened);
  };

  return (
    <ScratchpadContext.Provider value={{
      isScratchpadVisible,
      localStorageItem,
      setScratchpadVisibility,
      toggleScratchpadVisibility,
    }}>
      {children}
    </ScratchpadContext.Provider>
  );
};

ScratchpadProvider.propTypes = {
  children: PropTypes.node.isRequired,
  loginName: PropTypes.string.isRequired,
};
