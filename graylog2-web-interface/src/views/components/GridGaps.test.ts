import { findGaps } from './GridGaps';

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

    expect(findGaps(items)).toEqual([{
      start: { x: 1, y: 0 },
      end: { x: 3, y: 1 },
    }]);
  });

  it('does not find gaps for two items below each other occupying max width', () => {
    const items = [
      {
        start: { x: 1, y: 1 },
        end: { x: Infinity, y: 3 },
      },
      {
        start: { x: 1, y: 4 },
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

    expect(findGaps(items)).toEqual([
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
    ]);
  });

  it('finds initial gap', () => {
    const items = [
      {
        start: { x: 3, y: 0 },
        end: { x: 3, y: 1 },
      },
    ];

    expect(findGaps(items)).toEqual([{
      start: { x: 0, y: 0 },
      end: { x: 2, y: 1 },
    },
    ]);
  });
});
