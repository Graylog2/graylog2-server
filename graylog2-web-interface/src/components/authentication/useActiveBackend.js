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

  useEffect(() => {
    _loadActive();
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
