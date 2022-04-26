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
import { createContext, useState, useMemo, useCallback } from 'react';
import PropTypes from 'prop-types';

import Store from 'logic/local-storage/Store';

export const ScratchpadContext = createContext();

export const ScratchpadProvider = ({ children, loginName }) => {
  const localStorageItem = `gl-scratchpad-${loginName}`;
  const scratchpadStore = Store.get(localStorageItem) || {};
  const [isScratchpadVisible, setVisibility] = useState(scratchpadStore.opened || false);

  const toggleScratchpadVisibility = useCallback(() => {
    const currentStorage = Store.get(localStorageItem);

    Store.set(localStorageItem, { ...currentStorage, opened: !isScratchpadVisible });
    setVisibility(!isScratchpadVisible);
  }, [isScratchpadVisible, localStorageItem]);

  const setScratchpadVisibility = useCallback((opened) => {
    const currentStorage = Store.get(localStorageItem);

    Store.set(localStorageItem, { ...currentStorage, opened });
    setVisibility(opened);
  }, [localStorageItem]);

  const scratchpadContextValue = useMemo(() => ({
    isScratchpadVisible,
    localStorageItem,
    setScratchpadVisibility,
    toggleScratchpadVisibility,
  }), [
    isScratchpadVisible,
    localStorageItem,
    setScratchpadVisibility,
    toggleScratchpadVisibility,
  ]);

  return (
    <ScratchpadContext.Provider value={scratchpadContextValue}>
      {children}
    </ScratchpadContext.Provider>
  );
};

ScratchpadProvider.propTypes = {
  children: PropTypes.node.isRequired,
  loginName: PropTypes.string.isRequired,
};
