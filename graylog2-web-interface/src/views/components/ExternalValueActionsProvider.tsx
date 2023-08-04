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
import { useMemo } from 'react';

import usePluginEntities from 'hooks/usePluginEntities';
import type { ActionDefinition } from 'views/components/actions/ActionHandler';

import ExternalValueActionsContext from './ExternalValueActionsContext';

const usePluginExternalActions = () => {
  const useExternalActions = usePluginEntities('useExternalActions');
  const useExternalAction = useMemo<()=>({
    isLoading: boolean,
    externalValueActions: Array<ActionDefinition>,
    isError: boolean,
  })>(() => ((useExternalActions && typeof useExternalActions[0] === 'function') ? useExternalActions[0] : () => ({
      isLoading: false,
      externalValueActions: [],
      isError: false,
    })), [useExternalActions]);

  const { isLoading, externalValueActions, isError } = useExternalAction();

  return useMemo(() => ({ isLoading, isError, externalValueActions }), [externalValueActions, isError, isLoading]);
};

const ExternalValueActionsProvider = ({ children }) => {
  const contextValue = usePluginExternalActions();

  return (
    <ExternalValueActionsContext.Provider value={contextValue}>
      {children}
    </ExternalValueActionsContext.Provider>
  );
};

ExternalValueActionsProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default ExternalValueActionsProvider;
