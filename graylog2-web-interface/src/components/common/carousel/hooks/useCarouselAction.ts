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
import { useCallback, useState, useEffect } from 'react';

import useCarouselApi from './useCarouselApi';

const useCarouselAction = (carouselElementClass: string) => {
  const carouselApi = useCarouselApi(carouselElementClass);
  const [nextBtnDisabled, setNextBtnDisabled] = useState(true);
  const [prevBtnDisabled, setPrevBtnDisabled] = useState(true);

  const onSelect = useCallback(() => {
    setPrevBtnDisabled(!carouselApi.canScrollPrev());
    setNextBtnDisabled(!carouselApi.canScrollNext());
  }, [carouselApi]);

  useEffect(() => {
    if (!carouselApi) return;

    onSelect();
    carouselApi.on('reInit', onSelect);
    carouselApi.on('select', onSelect);
  }, [carouselApi, onSelect]);

  const scrollPrev = useCallback(() => {
    if (carouselApi) carouselApi.scrollPrev();
  }, [carouselApi]);
  const scrollNext = useCallback(() => {
    if (carouselApi) carouselApi.scrollNext();
  }, [carouselApi]);

  return {
    scrollNext,
    scrollPrev,
    nextBtnDisabled,
    prevBtnDisabled,
  };
};

export default useCarouselAction;
