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

type Options = {
  /** The element whose visible area the target is checked against. Defaults to the viewport. */
  root?: React.RefObject<HTMLElement>;
  rootMargin?: string;
  threshold?: number;
  /** Returned until the first observation. */
  initialIsIntersecting?: boolean;
};

/**
 * Returns whether the target element intersects with its root, e.g. whether it is visible in a scroll container.
 */
const useIntersectionObserver = (
  targetRef: React.RefObject<HTMLElement>,
  { root = undefined, rootMargin = undefined, threshold = undefined, initialIsIntersecting = false }: Options = {},
) => {
  const [isIntersecting, setIsIntersecting] = useState(initialIsIntersecting);

  useEffect(() => {
    const target = targetRef.current;

    if (!target) {
      return undefined;
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        setIsIntersecting(entry.isIntersecting);
      },
      { root: root?.current, rootMargin, threshold },
    );
    observer.observe(target);

    return () => observer.disconnect();
  }, [root, rootMargin, targetRef, threshold]);

  return isIntersecting;
};

export default useIntersectionObserver;
