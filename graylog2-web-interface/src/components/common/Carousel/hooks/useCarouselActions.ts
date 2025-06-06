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
  const [prevBtnDisabled, setPrevBtnDisabled] = useState(true);
  const [nextBtnDisabled, setNextBtnDisabled] = useState(true);

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

  return {
    prevBtnDisabled,
    nextBtnDisabled,
    scrollNext: carouselApi?.scrollNext ? carouselApi.scrollNext : () => {},
    scrollPrev: carouselApi?.scrollPrev ? carouselApi.scrollPrev : () => {},
  };
};

export default useCarouselActions;
