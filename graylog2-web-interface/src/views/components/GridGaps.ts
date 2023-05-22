/* eslint-disable no-plusplus */
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
import uniq from 'lodash/uniq';

const range = (start: number, end: number): Array<number> => [
  ...Array((end + 1) - start).keys(),
].map((i) => i + start);

const normalizeInfinity = ({ width, height, col, row }: Position, maxWidth: number): Position => ({
  col,
  row,
  height,
  width: Number.isFinite(width) ? width : maxWidth,
});

type Grid<T> = Array<Array<T>>;

const placeItemInGrid = <T extends boolean | string | number>(grid: Grid<T>, item: Position, value: T, overlapValue: T = value) => {
  let overlap = false;

  range(item.col, item.col + item.width - 1).forEach((x) => {
    range(item.row, item.row + item.height - 1).forEach((y) => {
      if (grid[y]?.[x] !== undefined) {
        overlap = true;
      }

      // eslint-disable-next-line no-param-reassign
      (grid[y] ??= [])[x] = (grid[y]?.[x] !== undefined) ? overlapValue : value;
    });
  });

  return overlap;
};

const itemsOverlap = (items: Position[]) => {
  if (items.length === 0) {
    return false;
  }

  const grid = [];

  // eslint-disable-next-line no-restricted-syntax
  for (const item of items) {
    if (placeItemInGrid(grid, item, true)) {
      return true;
    }
  }

  return false;
};

type Position = {
  col: number,
  row: number,
  height: number,
  width: number,
};

const rowIsEmpty = <T extends boolean | string | number>(grid: Grid<T>, row: number) => (grid[row] ?? []).every((cell) => cell === undefined);

const findGaps = (_items: Position[], minWidth: number = 1, maxWidth: number = 13): Position[] => {
  if (_items.length === 0) {
    return [];
  }

  const items = _items.map((item) => normalizeInfinity(item, maxWidth));
  const minY = Math.min(...items.map(({ row }) => row));
  const maxY = Math.max(...items.map(({ row, height }) => row + height - 1));

  if (itemsOverlap(items)) {
    return [];
  }

  const grid = [];

  items.forEach((item) => placeItemInGrid(grid, item, true));

  const gaps = [];

  for (let x = minWidth; x < maxWidth - 1; x++) {
    for (let y = minY; y <= maxY; y++) {
      if (!rowIsEmpty(grid, y) && grid[y]?.[x] === undefined) {
        const gap = { col: x, row: y, height: 1, width: 1 };

        while (gap.col + gap.width < maxWidth && grid[y]?.[gap.col + gap.width] === undefined) {
          gap.width += 1;
        }

        if (gap.width > 1) {
          while (!rowIsEmpty(grid, gap.row + gap.height) && gap.row + gap.height <= maxY) {
            if (range(gap.col, gap.col + gap.width - 1).every((k) => !grid[gap.row + gap.height]?.[k])) {
              gap.height += 1;
            } else {
              break;
            }
          }

          placeItemInGrid(grid, gap, true);

          gaps.push(gap);
        }
      }
    }
  }

  return uniq(gaps);
};

export default findGaps;
