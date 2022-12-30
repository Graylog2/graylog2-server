import findGaps from './GridGaps';

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
    ];

    expect(findGaps(items)).toContainEqual({
      start: { x: 1, y: 0 },
      end: { x: 3, y: 1 },
    });
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
    ];

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
    ];

    expect(findGaps(items)).toContainEqual(
      {
        start: {
          x: 4,
          y: 1,
        },
        end: {
          x: 7,
          y: 5,
        },
      });
  });

  it('finds initial gap', () => {
    const items = [
      {
        start: { x: 3, y: 1 },
        end: { x: 12, y: 2 },
      },
    ];

    expect(findGaps(items)).toContainEqual({
      start: { x: 1, y: 1 },
      end: { x: 3, y: 2 },
    });
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
    ];

    expect(findGaps(items)).toEqual([{
      start: { x: 1, y: 1 },
      end: { x: 5, y: 5 },
    }]);
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
    ];

    it('leading gaps', () => {
      const result = findGaps(items);

      expect(result).toContainEqual({
        start: {
          x: 1,
          y: 2,
        },
        end: {
          x: 5,
          y: 6,
        },
      });

      expect(result).toContainEqual({
        start: {
          x: 1,
          y: 6,
        },
        end: {
          x: 4,
          y: 10,
        },
      });
    });

    it('trailing gaps', () => {
      const result = findGaps(items);

      expect(result).toContainEqual({
        start: {
          x: 9,
          y: 2,
        },
        end: {
          x: 13,
          y: 6,
        },
      });

      expect(result).toContainEqual({
        start: {
          x: 8,
          y: 6,
        },
        end: {
          x: 13,
          y: 10,
        },
      });
    });
  });
});
