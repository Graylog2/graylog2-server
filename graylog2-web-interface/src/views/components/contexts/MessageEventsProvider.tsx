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
import Immutable from 'immutable';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';

import MessageEventsContext from './MessageEventsContext';

const getMessageEventsContextValue = (securityContent) => {
  if (!securityContent) {
    return undefined;
  }

  const initialContextValue = {
    eventTypes: Immutable.Map(),
    eventActions: Immutable.Map(),
    fieldValueActions: Immutable.Map(),
  };

  return securityContent.reduce((contextValue, secContent) => {
    const eventTypes = secContent.eventTypes.reduce((eventTypesById, eventType) => {
      return eventTypesById.set(eventType.gl2EventTypeCode, eventType);
    }, contextValue.eventTypes);

    const eventActions = secContent.externalActions.reduce((eventActionsById, eventAction) => {
      return eventActionsById.set(eventAction.id, eventAction);
    }, contextValue.eventActions);

    const fieldValueActions = eventActions.reduce((actionsByField, eventAction) => {
      let newEventActionsByField = actionsByField;

      eventAction.fields.forEach((field) => {
        const existingFieldValueActions = actionsByField.get(field) ?? Immutable.Map();
        newEventActionsByField = newEventActionsByField.set(field, existingFieldValueActions.merge(Immutable.Map({ [eventAction.id]: eventAction })));
      });

      return newEventActionsByField;
    }, contextValue.fieldValueActions);

    return {
      eventTypes,
      eventActions,
      fieldValueActions,
    };
  }, initialContextValue);
};

const MessageEventsProvider = ({ children }: { children: React.ReactElement }): React.ReactElement => {
  const securityContent = PluginStore.exports('securityContent');
  const contextValue = getMessageEventsContextValue(securityContent);

  return contextValue
    ? (
      <MessageEventsContext.Provider value={contextValue}>
        {children}
      </MessageEventsContext.Provider>
    )
    : children;
};

MessageEventsProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default MessageEventsProvider;
