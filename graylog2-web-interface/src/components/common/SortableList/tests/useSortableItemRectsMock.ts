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

const getSortableItemId = (element: Element) => {
  const childWithId = element.querySelector?.('[data-sortable-index]');

  const sortableIndex = childWithId?.getAttribute('data-sortable-index') ?? undefined;

  return sortableIndex ? Number(sortableIndex) : undefined;
};

const createRect = (index: number, height: number, width: number): DOMRect => {
  const top = index * height;

  return DOMRect.fromRect({
    x: 0,
    y: top,
    width,
    height,
  });
};

const useSortableItemRectsMock = ({
  height = 10,
  width = 100,
}: {
  height?: number;
  width?: number;
} = {}) => {
  let spy: jest.SpyInstance | undefined;

  beforeEach(() => {
    const originalGetBoundingClientRect = Element.prototype.getBoundingClientRect;

    spy = jest.spyOn(Element.prototype, 'getBoundingClientRect').mockImplementation(function mockRect(this: Element) {
      const sortableIndex = getSortableItemId(this);

      if (sortableIndex !== undefined) {
        return createRect(sortableIndex, height, width);
      }

      return originalGetBoundingClientRect.call(this);
    });
  });

  afterEach(() => {
    spy?.mockRestore();
  });
};

export default useSortableItemRectsMock;
