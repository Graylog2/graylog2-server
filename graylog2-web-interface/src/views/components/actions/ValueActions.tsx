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
import { useContext, useMemo } from 'react';

import FieldType from 'views/logic/fieldtypes/FieldType';
import type { QueryId } from 'views/logic/queries/Query';
import Action from 'views/components/actions/Action';
import { ActionContext } from 'views/logic/ActionContext';

type Props = {
  children: React.ReactNode,
  element: React.ReactNode,
  field: string,
  menuContainer?: HTMLElement | undefined | null
  queryId: QueryId,
  type?: FieldType
  value: React.ReactNode,
};

const ValueActions = ({ children, element, field, menuContainer = document.body, queryId, type = FieldType.Unknown, value }: Props) => {
  const actionContext = useContext(ActionContext);
  const handlerArgs = useMemo(() => ({ queryId, field, type, value, contexts: actionContext }), [actionContext, field, queryId, type, value]);
  const elementWithStatus = (() => element) as React.ComponentType<{ active: boolean }>;

  return (
    <Action element={elementWithStatus} handlerArgs={handlerArgs} menuContainer={menuContainer} type="value">
      {children}
    </Action>
  );
};

export default ValueActions;
