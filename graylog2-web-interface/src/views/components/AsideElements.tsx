import * as React from 'react';

import usePluginEntities from 'hooks/usePluginEntities';

const AsideElements = () => {
  const asideElements = usePluginEntities('views.elements.aside');

  return (
    <>
      {asideElements.map((Component, idx) => (
        // eslint-disable-next-line react/no-array-index-key
        <Component key={idx} />
      ))}
    </>
  );
};

export default AsideElements;
