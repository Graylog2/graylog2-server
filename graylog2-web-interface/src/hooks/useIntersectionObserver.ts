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

import { useEffect, useState } from 'react';

const useIntersectionObserver = (
  containerRef: React.RefObject<HTMLDivElement>,
  targetElementRef: React.RefObject<HTMLDivElement>,
) => {
  const [elementIsVisible, setElementIsVisible] = useState(false);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        setElementIsVisible(!entry.isIntersecting);
      },
      { threshold: 0.9, root: containerRef.current },
    );

    const indicatorElement = targetElementRef.current;

    if (indicatorElement) {
      observer.observe(indicatorElement);
    }

    return () => {
      if (indicatorElement) {
        observer.unobserve(indicatorElement);
      }
    };
  }, [containerRef, targetElementRef]);

  return elementIsVisible;
};

export default useIntersectionObserver;
