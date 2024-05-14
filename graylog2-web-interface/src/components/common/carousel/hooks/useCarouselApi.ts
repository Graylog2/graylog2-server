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

import { useContext } from 'react';

import CarouselApiContext from 'components/common/carousel/CarouselContext';

const useCarouselApi = (carouselId: string) => {
  const carouselApiContext = useContext(CarouselApiContext);

  if (!carouselApiContext) {
    throw new Error('useCarouselApi hook needs to be used inside CarouselApiProvider.');
  }

  if (!carouselApiContext[carouselId]) {
    throw new Error(`CarouselApiContext does not contain anything for carousel id ${carouselId}`);
  }

  return carouselApiContext[carouselId].api;
};

export default useCarouselApi;
