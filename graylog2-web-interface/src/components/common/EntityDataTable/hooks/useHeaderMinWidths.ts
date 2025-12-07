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

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import useObservedElementDimensions from 'hooks/useObservedElementDimensions';

const useHeaderMinWidths = () => {
  const refCallbacksRef = useRef<Map<string, (targetHeaderSection: HTMLDivElement | null) => void>>(new Map());
  const { register, dimensions } = useObservedElementDimensions<HTMLDivElement>();
  const [headerMinWidths, setHeaderMinWidths] = useState<{ [colId: string]: number }>({});
  const separator = useMemo(() => '__header-section__', []);

  const registerHeaderSection = useCallback(
    (colId: string, part: 'left' | 'right') => {
      const callbackKey = `${colId}${separator}${part}`;
      const existing = refCallbacksRef.current.get(callbackKey);

      if (existing) {
        return existing;
      }

      const observedRefCallback = register(callbackKey);

      const refCallback = (targetHeaderSection: HTMLDivElement | null) => {
        console.log('registerHeaderPart called for colId:', colId, 'part:', part, 'tableHeader:', targetHeaderSection);
        observedRefCallback(targetHeaderSection);
      };

      refCallbacksRef.current.set(callbackKey, refCallback);

      return refCallback;
    },
    [register, separator],
  );

  useEffect(() => {
    const aggregated: { [colId: string]: number } = {};

    Object.entries(dimensions).forEach(([key, { width }]) => {
      const [colId, part] = key.split(separator);
      if (!colId || !part) return;
      aggregated[colId] = (aggregated[colId] ?? 0) + Math.round(width);
    });

    setHeaderMinWidths((cur) => {
      const keysMatch =
        Object.keys(cur).length === Object.keys(aggregated).length &&
        Object.entries(aggregated).every(([k, v]) => cur[k] === v);

      return keysMatch ? cur : aggregated;
    });
  }, [dimensions, separator]);

  return { registerHeaderSection, headerMinWidths };
};

export default useHeaderMinWidths;
