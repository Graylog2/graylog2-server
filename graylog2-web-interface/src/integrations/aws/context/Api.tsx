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

// TODO: Fix typing
export const ApiContext = createContext<any>(undefined);

type ApiProviderProps = {
  children: any;
};

export const ApiProvider = ({
  children,
}: ApiProviderProps) => {
  const [availableRegions, setRegionsState] = useState([]);
  const [availableStreams, setStreamsState] = useState([]);
  const [availableGroups, setGroupsState] = useState([]);
  const [logData, setLogDataState] = useState(null);

  const setRegions = (results) => setRegionsState(results.regions);

  const setGroups = (results) => {
    const groups = results.log_groups.map((group) => ({ value: group, label: group }));
    setGroupsState(groups);
  };

  const setStreams = (results) => {
    const streams = results.streams.map((stream) => ({ value: stream, label: stream }));
    setStreamsState(streams);
  };

  const setLogData = (response) => {
    setLogDataState({
      message: JSON.stringify(response.message_fields, null, 2),
      type: response.input_type,
      additional: response.explanation,
    });
  };

  const clearLogData = () => {
    setLogDataState(null);
  };

  return (
    <ApiContext.Provider value={{
      availableStreams,
      setStreams,
      availableRegions,
      setRegions,
      logData,
      setLogData,
      clearLogData,
      availableGroups,
      setGroups,
    }}>
      {children}
    </ApiContext.Provider>
  );
};
