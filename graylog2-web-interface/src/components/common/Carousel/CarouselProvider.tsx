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
import { useMemo, useContext } from 'react';
import useEmblaCarousel from 'embla-carousel-react';

import CarouselContext from './CarouselContext';

type Props = React.PropsWithChildren<{
  carouselId: string,
  options?: Partial<{
    align: 'start',
    slidesToScroll: number,
    inViewThreshold: number,
    watchDrag: boolean
  }>
}>

const CarouselProvider = ({ carouselId, children, options = {} } : Props) => {
  const existingContextValue = useContext(CarouselContext);
  const [ref, api] = useEmblaCarousel(options);

  const value = useMemo(() => ({
    ...(existingContextValue ?? {}),
    [carouselId]: { ref, api },
  }), [api, carouselId, existingContextValue, ref]);

  return (
    <CarouselContext.Provider value={value}>
      {children}
    </CarouselContext.Provider>
  );
};

export default CarouselProvider;
