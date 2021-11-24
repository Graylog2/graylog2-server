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
import { useEffect, useContext } from 'react';
import { useFormikContext } from 'formik';
import { isEqual } from 'lodash';

import FormWarningsContext from 'contexts/FormWarningsContext';
import { QueryValidationState } from 'views/stores/QueriesStore';

const useSyncState = ({
  statusType,
  currentState,
  updateCurrentState,
  nextState,
}) => {
  useEffect(() => {
    if (nextState?.status === statusType && !isEqual(currentState, nextState)) {
      updateCurrentState(nextState);
    }

    if (currentState && nextState?.status !== statusType) {
      updateCurrentState(undefined);
    }
  }, [nextState, currentState, updateCurrentState, statusType]);
};

export const useSyncFormErrors = (validationState: QueryValidationState | undefined) => {
  const { errors, setFieldError } = useFormikContext<{ queryString: string }>();

  useSyncState({
    statusType: 'ERROR',
    nextState: validationState,
    currentState: errors.queryString,
    updateCurrentState: (nextState) => setFieldError('queryString', nextState),
  });
};

export const useSyncFormWarnings = (validationState: QueryValidationState | undefined) => {
  const { warnings, setFieldWarning } = useContext(FormWarningsContext);

  useSyncState({
    statusType: 'WARNING',
    nextState: validationState,
    currentState: warnings.queryString,
    updateCurrentState: (nextState) => setFieldWarning('queryString', nextState),
  });
};
