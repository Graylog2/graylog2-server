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

import type { MessageEventTypes } from 'views/types/messageEventTypes';

import MessageEventTypesContext from './MessageEventTypesContext';

const mergeMessageEventTypeLists = (messageEventTypeLists: Array<MessageEventTypes>) => {
  if (!messageEventTypeLists) {
    return undefined;
  }

  return messageEventTypeLists.reduce((allEventTypes, eventTypes) => ({ ...allEventTypes, ...eventTypes }), {});
};

type Props = {
  children: React.ReactElement
};

const MessageEventTypesProvider = ({ children }: Props) => {
  const eventTypeLists = PluginStore.exports('messageEventTypes');
  const eventTypes = mergeMessageEventTypeLists(eventTypeLists);

  return eventTypes
    ? (
      <MessageEventTypesContext.Provider value={{ eventTypes }}>
        {children}
      </MessageEventTypesContext.Provider>
    )
    : children;
};

MessageEventTypesProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default MessageEventTypesProvider;
