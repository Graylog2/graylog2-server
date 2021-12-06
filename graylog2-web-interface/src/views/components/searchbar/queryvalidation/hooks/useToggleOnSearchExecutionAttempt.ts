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
import { useState, useCallback, useEffect } from 'react';
import { delay } from 'lodash';

import QueryValidationActions from 'views/actions/QueryValidationActions';

const useToggleOnSearchExecutionAttempt = (showExplanation, setShowExplanation) => {
  const [shakingPopover, setShakingPopover] = useState(false);

  const shakePopover = useCallback(() => {
    if (!shakingPopover) {
      setShakingPopover(true);
      delay(() => setShakingPopover(false), 820);
    }
  }, [shakingPopover]);

  useEffect(() => {
    const unsubscribe = QueryValidationActions.displayValidationErrors.listen(() => {
      if (!showExplanation) {
        setShowExplanation(true);
      }

      if (showExplanation) {
        shakePopover();
      }
    });

    return () => {
      unsubscribe();
    };
  }, [showExplanation, shakingPopover, shakePopover, setShowExplanation]);

  return shakingPopover;
};

export default useToggleOnSearchExecutionAttempt;
