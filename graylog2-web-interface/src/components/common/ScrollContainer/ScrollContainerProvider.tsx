import { useMemo } from 'react';
import * as React from 'react';

import ScrollContainerContext from './ScrollContainerContext';

type Props = React.PropsWithChildren<{
  container: React.RefObject<HTMLDivElement>;
}>;

const ScrollContainerProvider = ({ children, container }: Props) => {
  const contextValue = useMemo(() => ({ container }), [container]);

  return <ScrollContainerContext.Provider value={contextValue}>{children}</ScrollContainerContext.Provider>;
};

export default ScrollContainerProvider;
