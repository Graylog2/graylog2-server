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
import { useContext } from 'react';

import FieldActionsContext, { type FieldActionsContextValue } from './FieldActionsContext';

const DEFAULT_CONTEXT_VALUE: FieldActionsContextValue = {
  evaluateCondition: (conditionFn, handlerArgs) => conditionFn(handlerArgs),
  additionalHandlerArgs: {},
  valueActions: [],
  fieldActions: [],
};

const useFieldActions = (): FieldActionsContextValue | undefined => {
  const contextValue = useContext(FieldActionsContext);

  if (!contextValue) {
    return DEFAULT_CONTEXT_VALUE;
  }

  return contextValue;
};

export default useFieldActions;
