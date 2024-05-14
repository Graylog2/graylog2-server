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

import { useState, useEffect, useCallback } from 'react';

import useCarouselApi from './useCarouselApi';

const useCarouselActions = (carouselId: string) => {
  const carouselApi = useCarouselApi(carouselId);
  const canScrollPrev = useCallback(() => !!carouselApi?.canScrollPrev(), [carouselApi]);
  const canScrollNext = useCallback(() => !!carouselApi?.canScrollNext(), [carouselApi]);

  const [nextBtnDisabled, setNextBtnDisabled] = useState(!canScrollPrev());
  const [prevBtnDisabled, setPrevBtnDisabled] = useState(!canScrollPrev());

  const onSelect = useCallback(() => {
    setPrevBtnDisabled(!canScrollPrev());
    setNextBtnDisabled(!canScrollNext());
  }, [canScrollNext, canScrollPrev]);

  useEffect(() => {
    carouselApi?.on('reInit', onSelect);
    carouselApi?.on('select', onSelect);
  }, [carouselApi, onSelect]);

  return {
    scrollNext: carouselApi?.scrollNext ? carouselApi.scrollNext : () => {},
    scrollPrev: carouselApi?.scrollPrev ? carouselApi.scrollPrev : () => {},
    nextBtnDisabled,
    prevBtnDisabled,
  };
};

export default useCarouselActions;
