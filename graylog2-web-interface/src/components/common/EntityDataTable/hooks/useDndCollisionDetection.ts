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
import type { DroppableContainer, ClientRect, UniqueIdentifier, CollisionDetection } from '@dnd-kit/core';
import { useRef, useCallback } from 'react';

const TARGET_ENTRY_RATIO = 0.5; // require pointer to cover at least 50% of the next column before switching

// Prefer pointer X, fall back to collision rect center (e.g., for keyboard sensor).
const resolvePointerX = (
  pointerCoordinates: { x: number } | null | undefined,
  collisionRect: { left: number; right: number } | null | undefined,
) => pointerCoordinates?.x ?? (collisionRect ? (collisionRect.left + collisionRect.right) / 2 : null);

// Build ordered rects for sortable columns, honoring current column order.
const buildRects = (droppableContainers: Array<DroppableContainer>, order: Array<string>) =>
  droppableContainers
    .map((container) => {
      const currentRect = container.rect.current;
      const rect = (
        'translated' in currentRect && currentRect.translated ? currentRect.translated : currentRect
      ) as ClientRect;
      const orderIndex = order.indexOf(container.id.toString());

      if (!rect || orderIndex === -1) {
        return null;
      }

      return { id: container.id, rect, orderIndex };
    })
    .filter((item) => !!item)
    .sort((a, b) => (a.rect.left === b.rect.left ? a.orderIndex - b.orderIndex : a.rect.left - b.rect.left));

const pickTargetId = (
  rects: Array<{ id: UniqueIdentifier; rect: { left: number; right: number } }>,
  pointerX: number,
  lastOverId: UniqueIdentifier | null,
) => {
  if (!rects.length) {
    return null;
  }
  if (rects.length === 1) {
    return rects[0].id;
  }

  const firstRect = rects[0];
  const lastRect = rects[rects.length - 1];

  if (pointerX < firstRect.rect.left) {
    return firstRect.id;
  }
  if (pointerX > lastRect.rect.right) {
    return lastRect.id;
  }

  const targetColIndex = rects.findIndex((rect) => rect.id === lastOverId);
  const current = rects[targetColIndex];

  // Stick with current target while inside its bounds.
  if (pointerX >= current.rect.left && pointerX <= current.rect.right) {
    return current.id;
  }

  // Move right only after crossing threshold into the next rect (with hysteresis).
  const next = rects[targetColIndex + 1];
  if (next && pointerX > current.rect.right) {
    const enterX = next.rect.left + (next.rect.right - next.rect.left) * TARGET_ENTRY_RATIO;

    if (pointerX >= enterX) {
      return next.id;
    }
  }

  // Move left only after crossing threshold into the previous rect (with hysteresis).
  const prev = rects[targetColIndex - 1];
  if (prev && pointerX < current.rect.left) {
    const enterX = prev.rect.right - (prev.rect.right - prev.rect.left) * TARGET_ENTRY_RATIO;

    if (pointerX <= enterX) {
      return prev.id;
    }
  }

  return current.id;
};

const useDndCollisionDetection = (draggableColumns: Array<string>) => {
  const lastOverId = useRef<UniqueIdentifier | null>(null);
  const setLastOverId = useCallback((id: UniqueIdentifier | null) => {
    lastOverId.current = id;
  }, []);

  const collisionDetection: CollisionDetection = useCallback(
    ({ droppableContainers, collisionRect, pointerCoordinates }) => {
      const pointerX = resolvePointerX(pointerCoordinates, collisionRect);

      if (pointerX == null) {
        return lastOverId.current ? [{ id: lastOverId.current }] : [];
      }

      const rects = buildRects(droppableContainers, draggableColumns);
      const targetId = pickTargetId(rects, pointerX, lastOverId.current);

      if (targetId) {
        lastOverId.current = targetId;

        return [{ id: targetId }];
      }

      return [];
    },
    [draggableColumns],
  );

  return {
    collisionDetection,
    setLastOverId,
  };
};

export default useDndCollisionDetection;
