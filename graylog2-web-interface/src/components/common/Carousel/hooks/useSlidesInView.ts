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
import { useCallback, useEffect, useState } from 'react';

import useCarouselApi from './useCarouselApi';

// Hook which returns all slide indices which have been in the view at least once.
const useSlidesInView = (carouselId: string) => {
  const carouselApi = useCarouselApi(carouselId);
  const [slidesInView, setSlidesInView] = useState<Array<number>>([]);

  const updateSlidesInView = useCallback(() => {
    setSlidesInView((cur) => {
      if (cur.length === carouselApi.slideNodes().length) {
        carouselApi.off('slidesInView', updateSlidesInView);
      }

      const inView = carouselApi
        .slidesInView()
        .filter((index) => !cur.includes(index));

      return cur.concat(inView);
    });
  }, [carouselApi]);

  useEffect(() => {
    if (!carouselApi) return;

    updateSlidesInView();
    carouselApi.on('slidesInView', updateSlidesInView);
    carouselApi.on('reInit', updateSlidesInView);
  }, [carouselApi, updateSlidesInView]);

  return slidesInView;
};

export default useSlidesInView;
