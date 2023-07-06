import React from 'react';

import useCarouselAction from 'hooks/useCarouselAction';
import { Icon } from 'components/common';
import { Button } from 'components/bootstrap';

const ContentStreamNewsContentActions = () => {
  const { scrollPrev, scrollNext } = useCarouselAction('.carousel');

  return (
    <>
      <Button onClick={() => scrollPrev()}><Icon name="arrow-left" /></Button>
      <Button onClick={() => scrollNext()}><Icon name="arrow-right" /></Button>
    </>
  );
};

export default ContentStreamNewsContentActions;
