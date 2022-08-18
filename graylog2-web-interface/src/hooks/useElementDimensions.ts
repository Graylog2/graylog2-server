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
import type * as React from 'react';
import { useState, useLayoutEffect } from 'react';
import useResizeObserver from '@react-hook/resize-observer';
import { debounce } from 'lodash';

// Simple hook which provides the width and height of an element by using a ResizeObserver.
const useElementDimensions = (target: React.RefObject<HTMLElement>, debounceDelay = 200) => {
  const [dimensions, setDimensions] = useState({ width: 0, height: 0 });
  const debouncedUpdate = debounce((newDimensions) => setDimensions(newDimensions), debounceDelay);

  useLayoutEffect(() => {
    if (target?.current) {
      const { width, height } = target.current.getBoundingClientRect();
      setDimensions({ width, height });
    }
  }, [target]);

  useResizeObserver(target, ({ contentRect: { width, height } }) => {
    debouncedUpdate({ width, height });
  });

  return dimensions;
};

export default useElementDimensions;
