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

import MessageEventTypesContext from './MessageEventTypesContext';

const getMessageEventTypesContextValue = (securityContent) => {
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

type Props = {
  children: React.ReactElement
};

const MessageEventTypesProvider = ({ children }: Props) => {
  const securityContent = PluginStore.exports('messageEventTypes');
  const contextValue = getMessageEventTypesContextValue(securityContent);

  return contextValue
    ? (
      <MessageEventTypesContext.Provider value={contextValue}>
        {children}
      </MessageEventTypesContext.Provider>
    )
    : children;
};

MessageEventTypesProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default MessageEventTypesProvider;
