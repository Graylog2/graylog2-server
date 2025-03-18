import { useContext, useEffect } from 'react';

import AutoRefreshContext from 'views/components/contexts/AutoRefreshContext';

const useOnRefresh = (fn: () => void) => {
  const context = useContext(AutoRefreshContext);
  useEffect(() => {
    if (context?.animationId !== null) {
      fn();
    }
  }, [context?.animationId, fn]);
};

export default useOnRefresh;
