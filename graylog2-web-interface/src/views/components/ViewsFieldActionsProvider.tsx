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
import { useCallback, useMemo } from 'react';

import usePluginEntities from 'hooks/usePluginEntities';
import type { AdditionalViewsActionHandlerArguments } from 'views/types';
import FieldActionsContext, { type FieldActionsContextValue } from 'views/components/actions/FieldActionsContext';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import type { ActionHandlerCondition, ResolvedActionHandlerArguments } from 'views/components/actions/ActionHandler';

const ViewsFieldActionsProvider = ({ children }: React.PropsWithChildren) => {
  const dispatch = useViewsDispatch();
  const queryId = useActiveQueryId();
  const valueActions = usePluginEntities('valueActions');
  const fieldActions = usePluginEntities('fieldActions');
  const additionalHandlerArgs = useMemo(() => ({ queryId }), [queryId]);
  const evaluateCondition = useCallback(
    (
      condition: ActionHandlerCondition<AdditionalViewsActionHandlerArguments>,
      args: ResolvedActionHandlerArguments<AdditionalViewsActionHandlerArguments>,
      fallbackValue: boolean,
    ) => {
      if (!condition) {
        return fallbackValue;
      }

      return dispatch((_dispatch, stateGetter) => condition(args, stateGetter));
    },
    [dispatch],
  );
  const executeThunkAction = useCallback((thunk, args) => Promise.resolve(dispatch(thunk(args))), [dispatch]);
  const actionConfig = useMemo(
    (): FieldActionsContextValue<AdditionalViewsActionHandlerArguments> => ({
      evaluateCondition,
      executeThunkAction,
      additionalHandlerArgs,
      valueActions,
      fieldActions,
    }),
    [evaluateCondition, executeThunkAction, additionalHandlerArgs, fieldActions, valueActions],
  );

  return <FieldActionsContext.Provider value={actionConfig}>{children}</FieldActionsContext.Provider>;
};

export default ViewsFieldActionsProvider;
