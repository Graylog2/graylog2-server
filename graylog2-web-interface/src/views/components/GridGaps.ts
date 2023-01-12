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

type Item = {
  start: { x: number, y: number },
  end: { x: number, y: number },
};

const range = (start: number, end: number): Array<number> => [
  ...Array((end + 1) - start).keys(),
].map((i) => i + start);

const itemsInRow = (items: Item[], row: number): Item[] => items.filter(({ start, end }) => start.y <= row && end.y > row);

function isRightToItem(item: Item, i: Item) {
  return item.start.x <= i.start.x;
}

const sortByStartXAsc = (item1: Item, item2: Item) => item1.start.x - item2.start.x;

const neighborsToRight = (item: Item, rowItems: { [row: string]: Item[] }) => {
  const rows = range(item.start.y, item.end.y - 1);
  const neighbors = rows.flatMap((row) => rowItems[row].filter((i) => i !== item).filter((i) => isRightToItem(item, i)).sort(sortByStartXAsc)[0])
    .filter((i) => i !== undefined);

  return uniq(neighbors);
};

const gapBetween = (item: Item, neighbor: Item): Item => ({
  start: { x: item.end.x, y: Math.min(item.start.y, neighbor.start.y) },
  end: { x: neighbor.start.x, y: Math.min(item.end.y, neighbor.end.y) },
});

const normalizeInfinity = ({ start, end }: Item, maxWidth: number): Item => ({
  start,
  end: {
    x: Number.isFinite(end.x) ? end.x : maxWidth,
    y: end.y,
  },
});

type RowItems = { [row: string]: Item[] };

const findInitialGaps = (rows: Array<number>, rowItems: RowItems, minWidth: number) => rows
  .filter((row) => rowItems[row].length > 0)
  .map((row) => [row, Math.min(...rowItems[row].map((item) => item.start.x))])
  .filter(([, startX]) => startX > minWidth)
  .reduce((gaps, [row, startX]) => {
    if (gaps.length === 0) {
      return [{ start: { x: minWidth, y: row }, end: { x: startX, y: row + 1 } }] as Item[];
    }

    const [gap, ...rest] = gaps.reverse();

    if (gap.end.x !== startX || gap.end.y < row) {
      return [...rest, gap, { start: { x: minWidth, y: row }, end: { x: startX, y: row + 1 } }];
    }

    return [...rest, { start: gap.start, end: { x: startX, y: row + 1 } }];
  }, [] as Item[]);

const findFinalGaps = (rows: Array<number>, rowItems: RowItems, maxWidth: number) => rows
  .filter((row) => rowItems[row].length > 0)
  .map((row) => [row, Math.max(...rowItems[row].map((item) => item.end.x))])
  .filter(([, endX]) => endX < maxWidth)
  .reduce((gaps, [row, endX]) => {
    if (gaps.length === 0) {
      return [{ start: { x: endX, y: row }, end: { x: maxWidth, y: row + 1 } }] as Item[];
    }

    const [gap, ...rest] = gaps.reverse();

    if (gap.start.x !== endX || gap.end.y < row) {
      return [...rest, gap, { start: { x: endX, y: row }, end: { x: maxWidth, y: row + 1 } }];
    }

    return [...rest, { start: gap.start, end: { x: maxWidth, y: row + 1 } }];
  }, [] as Item[]);

const itemsOverlap = (items: Item[]) => {
  if (items.length === 0) {
    return false;
  }

  const minY = Math.min(...items.map(({ start: { y } }) => y));
  const maxY = Math.max(...items.map(({ end: { y } }) => y - 1));

  const spaces = [];

  for (let row = minY; row <= maxY; row += 1) {
    spaces[row] = [];
  }

  // eslint-disable-next-line no-restricted-syntax
  for (const item of items) {
    const { start, end } = item;

    for (let { x } = start; x < end.x; x += 1) {
      for (let { y } = start; y < end.y; y += 1) {
        if (spaces[y][x] !== undefined) {
          return true;
        }

        spaces[y][x] = true;
      }
    }
  }

  return false;
};

const findGaps = (_items: Item[], minWidth: number = 1, maxWidth: number = 13): Item[] => {
  if (_items.length === 0) {
    return [];
  }

  const items = _items.map((item) => normalizeInfinity(item, maxWidth));

  if (itemsOverlap(items)) {
    return [];
  }

  const minY = Math.min(...items.map(({ start: { y } }) => y));
  const maxY = Math.max(...items.map(({ end: { y } }) => y - 1));

  const rows = range(minY, maxY);

  const rowItems = Object.fromEntries(rows.map((row) => [row, itemsInRow(items, row)]));

  const initialGaps = findInitialGaps(rows, rowItems, minWidth);

  const finalGaps = findFinalGaps(rows, rowItems, maxWidth);

  const gaps = items.flatMap((item) => {
    const neighbors = neighborsToRight(item, rowItems);

    return neighbors.filter((neighbor) => neighbor.start.x > item.end.x)
      .map((neighbor) => gapBetween(item, neighbor));
  });

  return uniq([...gaps, ...initialGaps, ...finalGaps]);
};

export default findGaps;
