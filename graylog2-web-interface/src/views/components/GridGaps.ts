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

const neighborsToRight = (item: Item, rowItems: Item[][]) => {
  const rows = range(item.start.y, item.end.y);
  const neighbors = rows.flatMap((row) => rowItems[row].filter((i) => i !== item).filter((i) => isRightToItem(item, i))[0])
    .filter((i) => i !== undefined);

  return uniq(neighbors);
};

const gapBetween = (item: Item, neighbor: Item): Item => ({
  start: { x: item.end.x + 1, y: Math.min(item.start.y, neighbor.start.y) },
  end: { x: neighbor.start.x - 1, y: Math.min(item.end.y, neighbor.end.y) },
});

const isFirstItemInRow = (items: Item[], item: Item) => {

};

export const findGaps = (items: Item[]): Item[] => {
  if (items.length === 0) {
    return [];
  }

  const minX = Math.min(...items.map(({ start: { x } }) => x));
  const maxX = Math.max(...items.map(({ end: { x } }) => x));
  const minY = Math.min(...items.map(({ start: { y } }) => y));
  const maxY = Math.max(...items.map(({ end: { y } }) => y));

  const rows = range(minY, maxY);

  const rowItems = rows.map((row) => itemsInRow(items, row));

  const initialGaps = items.filter((item) => isFirstItemInRow(items, item));
  const gaps = items.flatMap((item) => {
    const neighbors = neighborsToRight(item, rowItems);

    return neighbors.filter((neighbor) => neighbor.start.x > item.end.x)
      .map((neighbor) => gapBetween(item, neighbor));
  });

  return uniq(gaps);
};
