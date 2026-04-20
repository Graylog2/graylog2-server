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

import { useCallback, useLayoutEffect, useRef } from 'react';
import useResizeObserver from '@react-hook/resize-observer';

const useHeaderSectionObserver = (
  colId: string,
  part: 'left' | 'right',
  onWidth: (colId: string, part: 'left' | 'right', width: number) => void,
) => {
  const ref = useRef<HTMLDivElement>(null);

  const reportWidth = useCallback(
    (width: number) => {
      onWidth(colId, part, Math.round(width));
    },
    [colId, onWidth, part],
  );

  useLayoutEffect(() => {
    if (ref.current) {
      reportWidth(ref.current.getBoundingClientRect().width);
    }
  }, [reportWidth]);

  useResizeObserver(ref, ({ contentRect: { width } }) => reportWidth(width));

  return ref;
};

export default useHeaderSectionObserver;
