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
import * as Immutable from 'immutable';
import { ExternalEventAction, EventType } from 'views/types';

import { singleton } from 'views/logic/singleton';

type FieldName = string;

type MessageEventsContextType = {
  eventTypes: Immutable.Map<EventType['gl2EventTypeCode'], EventType>,
  eventActions: Immutable.Map<ExternalEventAction['id'], ExternalEventAction>,
  fieldValueActions: Immutable.Map<FieldName, Array<ExternalEventAction>>,
}

const MessageEventsContext = React.createContext<MessageEventsContextType | undefined>(undefined);

export default singleton('contexts.MessageEventsContext', () => MessageEventsContext);
