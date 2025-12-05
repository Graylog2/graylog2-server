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

import { useCallback, useEffect, useRef, useState } from 'react';

type HeaderParts = Map<string, { left?: HTMLDivElement; right?: HTMLDivElement }>;

const useHeaderMinWidths = () => {
  const headerSectionsRef = useRef<HeaderParts>(new Map());
  const resizeObserversRef = useRef<Map<HTMLDivElement, ResizeObserver>>(new Map());
  // This refs ensures we keep the ref callbacks stable per (colId, section).
  const refCallbacksRef = useRef<Map<string, (targetHeaderSection: HTMLDivElement | null) => void>>(new Map());
  const [headerMinWidths, setHeaderMinWidths] = useState<Record<string, number>>({});

  const updateWidth = useCallback((colId: string) => {
    const parts = headerSectionsRef.current.get(colId);
    const nextWidth = Math.round(
      (parts?.left?.getBoundingClientRect().width ?? 0) + (parts?.right?.getBoundingClientRect().width ?? 0),
    );

    setHeaderMinWidths((cur) => (cur[colId] === nextWidth ? cur : { ...cur, [colId]: nextWidth }));
  }, []);

  useEffect(
    () => () => {
      resizeObserversRef.current.forEach((observer) => observer.disconnect());
    },
    [],
  );

  const registerHeaderSection = useCallback(
    (colId: string, part: 'left' | 'right') => {
      const callbackKey = `${colId}-${part}`;
      const existing = refCallbacksRef.current.get(callbackKey);

      if (existing) {
        return existing;
      }

      let currentNode: HTMLDivElement | null = null;

      const refCallback = (targetHeaderSection: HTMLDivElement | null) => {
        const currentHeaderSections = headerSectionsRef.current.get(colId) ?? {};

        // If React swaps the DOM node (e.g. reorder/unmount/remount), drop the old observer before wiring the new one.
        if (currentNode && targetHeaderSection !== currentNode) {
          resizeObserversRef.current.get(currentNode)?.disconnect();
          resizeObserversRef.current.delete(currentNode);
          if (currentHeaderSections[part] === currentNode) {
            currentHeaderSections[part] = undefined;
          }
        }

        if (targetHeaderSection) {
          currentHeaderSections[part] = targetHeaderSection;
          currentNode = targetHeaderSection;
          if (!resizeObserversRef.current.has(targetHeaderSection)) {
            const observer = new ResizeObserver(() => updateWidth(colId));
            observer.observe(targetHeaderSection);
            resizeObserversRef.current.set(targetHeaderSection, observer);
          }
        } else {
          currentNode = null;
          currentHeaderSections[part] = undefined;
        }

        headerSectionsRef.current.set(colId, currentHeaderSections);
        updateWidth(colId);
      };

      refCallbacksRef.current.set(callbackKey, refCallback);

      return refCallback;
    },
    [updateWidth],
  );

  return { registerHeaderSection, headerMinWidths };
};

export default useHeaderMinWidths;
