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
// @flow strict
/* eslint-disable import/prefer-default-export */
import { useState, useEffect } from 'react';

import type { LoadActiveResponse } from 'actions/authentication/AuthenticationActions';
import type { ListenableAction } from 'stores/StoreTypes';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';

const useActiveBackend = <T>(listenableActions: Array<ListenableAction<T>> = []) => {
  const [loadActiveResponse, setLoadActiveResponse] = useState<?LoadActiveResponse>();
  const [finishedLoading, setFinishedLoading] = useState(false);
  const _loadActive = () => AuthenticationDomain.loadActive().then((response) => {
    setFinishedLoading(true);
    setLoadActiveResponse(response);
  });

  useEffect(() => { _loadActive(); }, []);

  useEffect(() => {
    const unlistenActions = listenableActions.map((action) => action.completed.listen(_loadActive));

    return () => {
      unlistenActions.forEach((unlistenAction) => unlistenAction());
    };
  }, [listenableActions]);

  return {
    finishedLoading,
    activeBackend: loadActiveResponse?.backend,
    backendsTotal: loadActiveResponse?.context?.backendsTotal,
  };
};

export default useActiveBackend;
