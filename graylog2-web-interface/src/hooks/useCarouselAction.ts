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
import EmblaCarousel from 'embla-carousel';
import { useCallback, useMemo, useState, useEffect } from 'react';

const useCarouselAction = (carouselElementClass: string) => {
  const [carousel, setCarousel] = useState<HTMLElement>(undefined);
  const [nextBtnDisabled, setNextBtnDisabled] = useState(true);
  const [prevBtnDisabled, setPrevBtnDisabled] = useState(true);

  const emblaApi = useMemo(() => carousel && EmblaCarousel(carousel, { containScroll: 'trimSnaps' }), [carousel]);

  const onSelect = useCallback(() => {
    setPrevBtnDisabled(!emblaApi.canScrollPrev());
    setNextBtnDisabled(!emblaApi.canScrollNext());
  }, [emblaApi]);

  useEffect(() => {
    if (!emblaApi) return;

    onSelect();
    emblaApi.on('reInit', onSelect);
    emblaApi.on('select', onSelect);
  }, [emblaApi, onSelect]);

  useEffect(() => {
    if (!carousel) {
      setInterval(() => {
        setCarousel(document.querySelector<HTMLElement>(carouselElementClass));
      }, 200);
    }
  }, [carousel, carouselElementClass]);

  const scrollPrev = useCallback(() => {
    if (emblaApi) emblaApi.scrollPrev();
  }, [emblaApi]);
  const scrollNext = useCallback(() => {
    if (emblaApi) emblaApi.scrollNext();
  }, [emblaApi]);

  return {
    scrollNext,
    scrollPrev,
    nextBtnDisabled,
    prevBtnDisabled,
  };
};

export default useCarouselAction;
