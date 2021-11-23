import { useState, useCallback, useEffect } from 'react';
import { delay } from 'lodash';

import { QueriesActions } from 'views/actions/QueriesActions';

const useToggleOnSearchExecutionAttempt = (showExplanation, setShowExplanation) => {
  const [shakingPopover, setShakingPopover] = useState(false);

  const shakePopover = useCallback(() => {
    if (!shakingPopover) {
      setShakingPopover(true);
      delay(() => setShakingPopover(false), 820);
    }
  }, [shakingPopover]);

  useEffect(() => {
    const unsubscribe = QueriesActions.displayValidationErrors.completed.listen(() => {
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
