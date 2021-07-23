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
import { ActionHandler, ActionHandlerArguments } from 'views/components/actions/ActionHandler';

type ExternalValueActionBase = {
  id: string,
  title: string,
  fields: Array<string>,
}

export type ExternalValueActionHttpGet = ExternalValueActionBase & {
  type: 'http_get'
  linkTarget: (externalAction: ExternalValueActionHttpGet, actionHandlerArgs: ActionHandlerArguments) => string,
  options: {
    action: string,
  }
}

export type ExternalValueActionLookupTable = ExternalValueActionBase & {
  type: 'lookup_table'
  handler: (externalAction: ExternalValueActionLookupTable, actionHandlerArgs: ActionHandlerArguments) => ReturnType<ActionHandler>,
  options: {
    lookupTableName: string,
  }
}

export type ExternalValueAction = ExternalValueActionHttpGet | ExternalValueActionLookupTable;
export type ExternalValueActions = { [valueActionId: string]: ExternalValueAction }
