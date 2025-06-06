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

/*
 * Hook which either returns
 * - all slides which are currently in view
 * - or all slides which have been in the view at least once.
 * depending on the `memorizePrevious` state.
 */
const useSlidesInView = (carouselId: string, memorizePrevious = false) => {
  const carouselApi = useCarouselApi(carouselId);
  const [slidesInView, setSlidesInView] = useState<Array<number>>([]);

  const updateSlidesInView = useCallback(() => {
    setSlidesInView((cur) => {
      if (cur.length === carouselApi.slideNodes().length) {
        carouselApi.off('slidesInView', updateSlidesInView);
      }

      const inView = carouselApi.slidesInView();

      if (memorizePrevious) {
        return cur.concat(inView.filter((index) => !cur.includes(index)));
      }

      return inView;
    });
  }, [carouselApi, memorizePrevious]);

  useEffect(() => {
    if (!carouselApi) return;

    updateSlidesInView();

    carouselApi.on('slidesInView', () => {
      updateSlidesInView();
    });
  }, [carouselApi, updateSlidesInView]);

  return slidesInView;
};

export default useSlidesInView;
