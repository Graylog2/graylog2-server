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
import * as React from 'react';
import { useMemo } from 'react';
import useEmblaCarousel from 'embla-carousel-react';

import CarouselApiContext from 'components/common/carousel/CarouselContext';

type Props = React.PropsWithChildren<{
  carouselId: string
}>

const CarouselProvider = ({ carouselId, children } : Props) => {
  const [ref, api] = useEmblaCarousel({ containScroll: 'trimSnaps' });

  const value = useMemo(() => ({
    [carouselId]: { ref, api },
  }), [api, carouselId, ref]);

  return (
    <CarouselApiContext.Provider value={value}>
      {children}
    </CarouselApiContext.Provider>
  );
};

export default CarouselProvider;
