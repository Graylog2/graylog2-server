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
import { PluginStore } from 'graylog-web-plugin/plugin';

import { ExternalValueActions } from 'views/types/externalValueActions';

import ExternalValueActionsContext from './ExternalValueActionsContext';

const mergeExternalValueActionLists = (externalValueActionLists) => {
  if (!externalValueActionLists) {
    return undefined;
  }

  return externalValueActionLists.reduce((allExternalActions, externalActions) => ({ ...allExternalActions, ...externalActions }), {});
};

const _getActionsForField = (externalValueActions: ExternalValueActions | undefined = {}, fieldName: string) => {
  return Object.values(externalValueActions).filter((valueAction) => valueAction.fields.includes(fieldName));
};

type Props = {
  children: React.ReactElement
};

const ExternalValueActionsProvider = ({ children }: Props) => {
  const messageEventTypeLists = PluginStore.exports('externalValueActions');
  const externalValueActions = mergeExternalValueActionLists(messageEventTypeLists);
  const getActionsForField = (fieldName) => _getActionsForField(externalValueActions, fieldName);

  return externalValueActions
    ? (
      <ExternalValueActionsContext.Provider value={{ externalValueActions, getActionsForField }}>
        {children}
      </ExternalValueActionsContext.Provider>
    )
    : children;
};

ExternalValueActionsProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default ExternalValueActionsProvider;
