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

const getSortableItemId = (element: Element): string | undefined => {
  const childWithId = element.querySelector?.('[data-sortable-id]');

  return childWithId?.getAttribute('data-sortable-id') ?? undefined;
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

const useSortableItemRectsMock = (
  items: Array<string>,
  {
    height = 10,
    width = 100,
  }: {
    height?: number;
    width?: number;
  } = {},
) => {
  let spy: jest.SpyInstance | undefined;

  beforeEach(() => {
    const originalGetBoundingClientRect = Element.prototype.getBoundingClientRect;

    spy = jest.spyOn(Element.prototype, 'getBoundingClientRect').mockImplementation(function mockRect(this: Element) {
      const sortableId = getSortableItemId(this);
      const index = sortableId ? items.findIndex((itemId) => itemId === sortableId) : -1;

      if (index >= 0) {
        return createRect(index, height, width);
      }

      return originalGetBoundingClientRect.call(this);
    });
  });

  afterEach(() => {
    spy?.mockRestore();
  });
};

export default useSortableItemRectsMock;
