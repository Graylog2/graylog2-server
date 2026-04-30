import { useCallback } from 'react';

import { useQueryParam, StringParam } from 'routing/QueryParams';

const useLayoutVariant = () => {
  const [layoutVariant, setActiveLayout] = useQueryParam('layout_variant', StringParam);
  const activeLayoutVariant = layoutVariant ?? '';

  const selectLayoutVariant = useCallback(
    (layout: string) => {
      setActiveLayout(layout === activeLayoutVariant ? undefined : layout);
    },
    [activeLayoutVariant, setActiveLayout],
  );

  const resetLayoutVariant = useCallback(() => {
    setActiveLayout(undefined);
  }, [setActiveLayout]);

  return {
    activeLayoutVariant,
    selectLayoutVariant,
    resetLayoutVariant,
  };
};

export default useLayoutVariant;
