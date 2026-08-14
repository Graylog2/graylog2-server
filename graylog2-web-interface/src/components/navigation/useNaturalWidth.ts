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
import { useCallback, useState } from 'react';
import useResizeObserver from '@react-hook/resize-observer';

/**
 * Width of an element which is only rendered some of the time, retained while it is not.
 *
 * `useElementDimensions` cannot be used for this: it subscribes to whichever element its ref held
 * during the first commit and never re-subscribes, so it neither follows a remounted element nor
 * ignores the zero-size notification a browser delivers for the element it watches once that leaves
 * the document. Keeping the element in state instead re-subscribes whenever it changes, and
 * discarding zero widths keeps the last real measurement available while the element is gone.
 */
const useNaturalWidth = <T extends HTMLElement>(): [(node: T | null) => void, number] => {
  const [width, setWidth] = useState(0);
  const [element, setElement] = useState<T | null>(null);

  const ref = useCallback((node: T | null) => {
    setElement(node);

    if (node) {
      setWidth(node.getBoundingClientRect().width);
    }
  }, []);

  useResizeObserver(element, ({ contentRect }) => {
    if (contentRect.width > 0) {
      setWidth(contentRect.width);
    }
  });

  return [ref, width];
};

export default useNaturalWidth;
