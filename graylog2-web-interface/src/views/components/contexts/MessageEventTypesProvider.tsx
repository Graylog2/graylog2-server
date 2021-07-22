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
import type { MessageEventType } from 'views/types';

import MessageEventTypesContext from './MessageEventTypesContext';

const getImmutableMessageEventTypes = () => {
  const messageEventTypes = PluginStore.exports('messageEventTypes');

  if (!messageEventTypes) {
    return undefined;
  }

  return Immutable.List(messageEventTypes).reduce((
    eventTypes,
    eventTypeMap,
  ) => eventTypes.merge(Immutable.Map(eventTypeMap)),
  Immutable.Map<MessageEventType['gl2EventTypeCode'], MessageEventType>());
};

type Props = {
  children: React.ReactElement
};

const MessageEventTypesProvider = ({ children }: Props) => {
  const messageEventTypes = getImmutableMessageEventTypes();

  return messageEventTypes
    ? (
      <MessageEventTypesContext.Provider value={{ eventTypes: messageEventTypes }}>
        {children}
      </MessageEventTypesContext.Provider>
    )
    : children;
};

MessageEventTypesProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default MessageEventTypesProvider;
