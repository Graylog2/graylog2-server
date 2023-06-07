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
import findGaps from './GridGaps';

type Item = {
  start: {
    x: number,
    y: number,
  },
  end: {
    x: number,
    y: number
  }
};

type Position = {
  col: number,
  row: number,
  width: number,
  height: number,
};
const convertItem = (item: Item): Position => ({
  col: item.start.x,
  row: item.start.y,
  width: item.end.x - item.start.x,
  height: item.end.y - item.start.y,
});

describe('GridGaps', () => {
  it('finds single gap between two items', () => {
    const items = [
      {
        start: { x: 0, y: 0 },
        end: { x: 1, y: 1 },
      },
      {
        start: { x: 3, y: 0 },
        end: { x: 4, y: 1 },
      },
    ].map(convertItem);

    expect(findGaps(items)).toContainEqual(convertItem({
      start: { x: 1, y: 0 },
      end: { x: 3, y: 1 },
    }));
  });

  it('does not find gaps for two items below each other occupying max width', () => {
    const items = [
      {
        start: { x: 1, y: 1 },
        end: { x: Infinity, y: 3 },
      },
      {
        start: { x: 1, y: 3 },
        end: { x: Infinity, y: 10 },
      },
    ].map(convertItem);

    expect(findGaps(items)).toEqual([]);
  });

  it('finds gaps between two widgets separated by multiple units', () => {
    const items = [
      {
        start: {
          x: 1,
          y: 1,
        },
        end: {
          x: 4,
          y: 5,
        },
      },
      {
        start: {
          x: 7,
          y: 1,
        },
        end: {
          x: 12,
          y: 5,
        },
      },
      {
        start: {
          x: 1,
          y: 5,
        },
        end: {
          x: Infinity,
          y: 11,
        },
      },
    ].map(convertItem);

    expect(findGaps(items)).toContainEqual(convertItem(
      {
        start: {
          x: 4,
          y: 1,
        },
        end: {
          x: 7,
          y: 5,
        },
      },
    ));
  });

  it('finds initial gap', () => {
    const items = [
      {
        start: { x: 3, y: 1 },
        end: { x: 12, y: 2 },
      },
    ].map(convertItem);

    expect(findGaps(items)).toContainEqual(convertItem({
      start: { x: 1, y: 1 },
      end: { x: 3, y: 2 },
    }));
  });

  it('calculates initial gap correctly', () => {
    const items = [
      {
        start: {
          x: 5,
          y: 1,
        },
        end: {
          x: 13,
          y: 5,
        },
      },
      {
        start: {
          x: 1,
          y: 5,
        },
        end: {
          x: 13,
          y: 11,
        },
      },
    ].map(convertItem);

    expect(findGaps(items)).toEqual([{
      start: { x: 1, y: 1 },
      end: { x: 5, y: 5 },
    }].map(convertItem));
  });

  describe('calculates overlapping gaps correctly', () => {
    const items = [
      {
        start: {
          x: 5,
          y: 2,
        },
        end: {
          x: 9,
          y: 6,
        },
      },
      {
        start: {
          x: 4,
          y: 6,
        },
        end: {
          x: 8,
          y: 10,
        },
      },
    ].map(convertItem);

    it('leading gaps', () => {
      const result = findGaps(items);

      expect(result).toContainEqual(convertItem({
        start: {
          x: 1,
          y: 2,
        },
        end: {
          x: 5,
          y: 6,
        },
      }));

      expect(result).toContainEqual(convertItem({
        start: {
          x: 1,
          y: 6,
        },
        end: {
          x: 4,
          y: 10,
        },
      }));
    });

    it('trailing gaps', () => {
      const result = findGaps(items);

      expect(result).toContainEqual(convertItem({
        start: {
          x: 9,
          y: 2,
        },
        end: {
          x: 13,
          y: 6,
        },
      }));

      expect(result).toContainEqual(convertItem({
        start: {
          x: 8,
          y: 6,
        },
        end: {
          x: 13,
          y: 10,
        },
      }));
    });
  });

  it('avoid overflowing into next row', () => {
    const items = [
      {
        start: {
          x: 11,
          y: 1,
        },
        end: {
          x: 13,
          y: 7,
        },
      },
      {
        start: {
          x: 7,
          y: 1,
        },
        end: {
          x: 11,
          y: 7,
        },
      },
      {
        start: {
          x: 1,
          y: 1,
        },
        end: {
          x: 4,
          y: 7,
        },
      },
    ].map(convertItem);

    expect(findGaps(items)).toEqual([{
      start: { x: 4, y: 1 },
      end: { x: 7, y: 7 },
    }].map(convertItem));
  });

  it('avoid occupying empty row(s)', () => {
    const items = [
      {
        start: {
          x: 1,
          y: 1,
        },
        end: {
          x: 4,
          y: 4,
        },
      },
      {
        start: {
          x: 1,
          y: 10,
        },
        end: {
          x: 13,
          y: 15,
        },
      },
    ].map(convertItem);

    expect(findGaps(items)).toEqual([{
      start: { x: 4, y: 1 },
      end: { x: 13, y: 4 },
    }].map(convertItem));
  });

  it('should not generate gaps for overlapping widgets', () => {
    const items = [
      {
        start: {
          x: 1,
          y: 1,
        },
        end: {
          x: 5,
          y: 5,
        },
      },
      {
        start: {
          x: 1,
          y: 1,
        },
        end: {
          x: Infinity,
          y: 3,
        },
      },
    ].map(convertItem);

    expect(findGaps(items)).toEqual([]);
  });

  it('should not overlap initial placeholders with other widgets', () => {
    const items = [{
      start: {
        x: 5,
        y: 1,
      },
      end: {
        x: 13,
        y: 5,
      },
    },

    {
      start: {
        x: 1,
        y: 5,
      },
      end: {
        x: 13,
        y: 7,
      },
    },
    {
      start: {
        x: 5,
        y: 7,
      },
      end: {
        x: 13,
        y: 11,
      },
    },
    ].map(convertItem);

    expect(findGaps(items)).toEqual([{
      start: {
        x: 1,
        y: 1,
      },
      end: {
        x: 5,
        y: 5,
      },
    }, {
      start: {
        x: 1,
        y: 7,
      },
      end: {
        x: 5,
        y: 11,
      },
    }].map(convertItem));
  });

  it('should not overlap final placeholders with other widgets', () => {
    const items = [
      {
        start: {
          x: 1,
          y: 1,
        },
        end: {
          x: 5,
          y: 5,
        },
      },
      {
        start: {
          x: 1,
          y: 5,
        },
        end: {
          x: 13,
          y: 7,
        },
      },
      {
        start: {
          x: 1,
          y: 7,
        },
        end: {
          x: 5,
          y: 11,
        },
      },
    ].map(convertItem);

    expect(findGaps(items)).toEqual([{
      start: {
        x: 5,
        y: 1,
      },
      end: {
        x: 13,
        y: 5,
      },
    }, {
      start: {
        x: 5,
        y: 7,
      },
      end: {
        x: 13,
        y: 11,
      },
    }].map(convertItem));
  });

  it('does not generate invalid gaps', () => {
    const items = [
      {
        start: {
          x: 1,
          y: 1,
        },
        end: {
          x: 5,
          y: 3,
        },
      },
      {
        start: {
          x: 5,
          y: 1,
        },
        end: {
          x: 9,
          y: 7,
        },
      },
      {
        start: {
          x: 1,
          y: 3,
        },
        end: {
          x: 5,
          y: 6,
        },
      },
      {
        start: {
          x: 1,
          y: 6,
        },
        end: {
          x: 4,
          y: 9,
        },
      },
    ].map(convertItem);

    const result = findGaps(items);

    expect(result).toHaveLength(2);

    expect(result).toEqual(expect.arrayContaining([
      {
        end: {
          x: 13,
          y: 7,
        },
        start: {
          x: 9,
          y: 1,
        },
      },
      {
        end: {
          x: 13,
          y: 9,
        },
        start: {
          x: 4,
          y: 7,
        },
      },
    ].map(convertItem)));
  });
});
