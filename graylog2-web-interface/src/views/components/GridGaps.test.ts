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
        end: { x: 3, y: 1 },
      },
    ];

    expect(findGaps(items)).toEqual([{
      start: { x: 2, y: 0 },
      end: { x: 2, y: 1 },
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
