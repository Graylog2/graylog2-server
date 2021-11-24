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
