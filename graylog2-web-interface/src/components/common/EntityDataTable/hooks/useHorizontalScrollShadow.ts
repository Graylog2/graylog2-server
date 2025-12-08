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

import type { RefObject } from 'react';
import { useCallback, useEffect, useState } from 'react';
import useResizeObserver from '@react-hook/resize-observer';

const EPSILON = 1;

const useCanScrollRight = (
  scrollContainerRef: RefObject<HTMLElement>,
  tableIsCompressed: boolean = false,
  recompute: unknown,
) => {
  const [canScrollRight, setCanScrollRight] = useState(false);

  const updateCanScrollRight = useCallback(() => {
    const scrollContainer = scrollContainerRef?.current;

    if (!scrollContainer) {
      return;
    }

    const canScrollHorizontally = scrollContainer.scrollWidth - scrollContainer.clientWidth > EPSILON;
    const isAtRightEdge =
      scrollContainer.scrollLeft + scrollContainer.clientWidth >= scrollContainer.scrollWidth - EPSILON;

    const shouldShowShadow = canScrollHorizontally && !isAtRightEdge;

    setCanScrollRight((currentState) => (currentState === shouldShowShadow ? currentState : shouldShowShadow));
  }, [scrollContainerRef]);

  useEffect(() => {
    const scrollContainer = scrollContainerRef?.current;

    if (!scrollContainer) {
      return undefined;
    }

    updateCanScrollRight();

    const handleScroll = () => updateCanScrollRight();

    scrollContainer.addEventListener('scroll', handleScroll);

    return () => scrollContainer.removeEventListener('scroll', handleScroll);
  }, [scrollContainerRef, updateCanScrollRight]);

  useResizeObserver(scrollContainerRef, updateCanScrollRight);

  useEffect(() => {
    updateCanScrollRight();
  }, [updateCanScrollRight, recompute]);

  return tableIsCompressed && canScrollRight;
};

export default useCanScrollRight;
