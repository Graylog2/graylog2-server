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

type Dimensions = { width: number; height: number };

const useObservedElementDimensions = <ElementT extends HTMLElement = HTMLElement>() => {
  const observedElementsRef = useRef<Map<string, ElementT>>(new Map());
  const resizeObserversRef = useRef<Map<ElementT, ResizeObserver>>(new Map());
  const refCallbacksRef = useRef<Map<string, (node: ElementT | null) => void>>(new Map());
  const [dimensions, setDimensions] = useState<Record<string, Dimensions>>({});

  const updateDimensions = useCallback((key: string, node: ElementT | null) => {
    if (!node) {
      setDimensions((cur) => {
        if (!(key in cur)) return cur;
        const { [key]: _unused, ...rest } = cur;
        return rest;
      });
      return;
    }

    const { width, height } = node.getBoundingClientRect();
    const next = { width: Math.round(width), height: Math.round(height) };

    setDimensions((cur) => {
      const prev = cur[key];
      if (prev && prev.width === next.width && prev.height === next.height) {
        return cur;
      }

      return { ...cur, [key]: next };
    });
  }, []);

  useEffect(
    () => () => {
      resizeObserversRef.current.forEach((observer) => observer.disconnect());
    },
    [],
  );

  const register = useCallback(
    (key: string) => {
      const existing = refCallbacksRef.current.get(key);

      if (existing) {
        return existing;
      }

      let currentNode: ElementT | null = null;

      const refCallback = (node: ElementT | null) => {
        if (currentNode && node !== currentNode) {
          resizeObserversRef.current.get(currentNode)?.disconnect();
          resizeObserversRef.current.delete(currentNode);
          observedElementsRef.current.delete(key);
        }

        if (node) {
          observedElementsRef.current.set(key, node);
          currentNode = node;
          updateDimensions(key, node);

          if (!resizeObserversRef.current.has(node)) {
            const observer = new ResizeObserver(() => updateDimensions(key, node));
            observer.observe(node);
            resizeObserversRef.current.set(node, observer);
          }
        } else {
          currentNode = null;
          observedElementsRef.current.delete(key);
          updateDimensions(key, null);
        }
      };

      refCallbacksRef.current.set(key, refCallback);

      return refCallback;
    },
    [updateDimensions],
  );

  return { register, dimensions };
};

export default useObservedElementDimensions;
