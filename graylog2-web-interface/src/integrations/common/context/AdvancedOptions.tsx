import React, { createContext, useMemo, useState } from 'react';

import type { AdvancedOptionsContextType } from '../utils/types';

export const AdvancedOptionsContext = createContext<AdvancedOptionsContextType>(null);

export const AdvancedOptionsProvider = ({ children = undefined }: React.PropsWithChildren<{}>) => {
  const [isAdvancedOptionsVisible, setAdvancedOptionsVisibility] = useState<boolean>(false);
  const advancedOptionsProvider = useMemo(
    () => ({
      isAdvancedOptionsVisible,
      setAdvancedOptionsVisibility,
    }),
    [isAdvancedOptionsVisible, setAdvancedOptionsVisibility],
  );

  return <AdvancedOptionsContext.Provider value={advancedOptionsProvider}>{children}</AdvancedOptionsContext.Provider>;
};
