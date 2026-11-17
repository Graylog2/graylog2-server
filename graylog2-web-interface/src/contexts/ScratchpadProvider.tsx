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
import { createContext, useState, useMemo } from 'react';

import Store from 'logic/local-storage/Store';
import useHotkey from 'hooks/useHotkey';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

type ScratchPadContextType = {
  isScratchpadVisible: boolean;
  localStorageItem: string;
  setScratchpadVisibility: (opened: boolean) => void;
  toggleScratchpadVisibility: () => void;
};
export const ScratchpadContext = createContext<ScratchPadContextType>(undefined);

type Props = {
  children: React.ReactNode;
  loginName: string;
};

export const ScratchpadProvider = ({ children, loginName }: Props) => {
  const localStorageItem = `gl-scratchpad-${loginName}`;
  type ScratchpadStoreData = Record<string, unknown> & { opened?: boolean };
  const scratchpadStore = Store.get<ScratchpadStoreData>(localStorageItem) || {};
  const [isScratchpadVisible, setVisibility] = useState(scratchpadStore.opened || false);
  const sendTelemetry = useSendTelemetry();

  const scratchpadContextValue = useMemo(() => {
    const setScratchpadVisibility = (opened: boolean) => {
      const currentStorage = Store.get<ScratchpadStoreData>(localStorageItem);

      Store.set(localStorageItem, { ...currentStorage, opened });
      setVisibility(opened);

      if (opened !== isScratchpadVisible) {
        sendTelemetry(opened ? TELEMETRY_EVENT_TYPE.SCRATCHPAD.OPENED : TELEMETRY_EVENT_TYPE.SCRATCHPAD.CLOSED, {
          app_section: 'scratchpad',
        });
      }
    };

    const toggleScratchpadVisibility = () => setScratchpadVisibility(!isScratchpadVisible);

    return {
      isScratchpadVisible,
      localStorageItem,
      setScratchpadVisibility,
      toggleScratchpadVisibility,
    };
  }, [isScratchpadVisible, localStorageItem, sendTelemetry]);

  useHotkey({
    actionKey: 'show-scratchpad-modal',
    callback: () => scratchpadContextValue.setScratchpadVisibility(true),
    scope: 'general',
    dependencies: [scratchpadContextValue],
  });

  return <ScratchpadContext.Provider value={scratchpadContextValue}>{children}</ScratchpadContext.Provider>;
};
