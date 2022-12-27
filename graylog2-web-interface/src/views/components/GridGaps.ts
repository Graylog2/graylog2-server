import uniq from 'lodash/uniq';

type Item = {
  start: { x: number, y: number },
  end: { x: number, y: number },
};

const range = (start: number, end: number): Array<number> => [
  ...Array((end + 1) - start).keys(),
].map((i) => i + start);

const itemsInRow = (items: Item[], row: number): Item[] => items.filter(({ start, end }) => start.y <= row && end.y >= row);

function isRightToItem(item: Item, i: Item) {
  return item.start.x <= i.start.x;
}

const neighborsToRight = (item: Item, rowItems: { [row: string]: Item[] }) => {
  const rows = range(item.start.y, item.end.y);
  const neighbors = rows.flatMap((row) => rowItems[row].filter((i) => i !== item).filter((i) => isRightToItem(item, i))[0])
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
  .map((row) => [row, Math.min(...rowItems[row].map((item) => item.start.x))])
  .filter(([, startX]) => startX > minWidth)
  .reduce((gaps, [row, startX]) => {
    if (gaps.length === 0) {
      return [{ start: { x: minWidth, y: row }, end: { x: startX, y: row } }] as Item[];
    }

    const [gap, ...rest] = gaps.reverse();

    if (gap.end.x !== startX) {
      return [...rest, gap, { start: { x: minWidth, y: row }, end: { x: startX, y: row } }];
    }

    return [...rest, { start: gap.start, end: { x: startX, y: row } }];
  }, [] as Item[]);

const findFinalGaps = (rows: Array<number>, rowItems: RowItems, maxWidth: number) => rows
  .map((row) => [row, Math.max(...rowItems[row].map((item) => item.end.x))])
  .filter(([, endX]) => endX < maxWidth)
  .reduce((gaps, [row, endX]) => {
    if (gaps.length === 0) {
      return [{ start: { x: endX, y: row }, end: { x: maxWidth + 1, y: row + 1 } }] as Item[];
    }

    const [gap, ...rest] = gaps.reverse();

    if (gap.start.x !== endX) {
      return [...rest, gap, { start: { x: endX, y: row }, end: { x: maxWidth + 1, y: row + 1 } }];
    }

    return [...rest, { start: gap.start, end: { x: maxWidth + 1, y: row + 1 } }];
  }, [] as Item[]);

export const findGaps = (_items: Item[], minWidth: number = 1, maxWidth: number = 12): Item[] => {
  if (_items.length === 0) {
    return [];
  }

  const items = _items.map((item) => normalizeInfinity(item, maxWidth));
  console.log({ items });
  const minY = Math.min(...items.map(({ start: { y } }) => y));
  const maxY = Math.max(...items.map(({ end: { y } }) => y));

  const rows = range(minY, maxY);

  const rowItems = Object.fromEntries(rows.map((row) => [row, itemsInRow(items, row)]));

  const initialGaps = findInitialGaps(rows, rowItems, minWidth);

  const finalGaps = findFinalGaps(rows, rowItems, maxWidth);

  console.log({ initialGaps, finalGaps });

  const gaps = items.flatMap((item) => {
    const neighbors = neighborsToRight(item, rowItems);

    return neighbors.filter((neighbor) => neighbor.start.x > item.end.x)
      .map((neighbor) => gapBetween(item, neighbor));
  });

  return uniq([...gaps, ...initialGaps, ...finalGaps]);
};
