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
