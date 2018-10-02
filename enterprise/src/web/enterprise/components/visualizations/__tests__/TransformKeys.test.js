import * as fixtures from './TransformKeys.fixtures';

const CurrentUserStore = { get: jest.fn() };
jest.doMock('injection/CombinedProvider', () => ({
  get: () => ({ CurrentUserStore }),
}));

// eslint-disable-next-line global-require
const loadSUT = () => require('../TransformKeys').default;
describe('TransformKeys', () => {
  beforeEach(() => {
    jest.resetAllMocks();
    jest.resetModules();
  });
  it('returns original result when no aggregations are present', () => {
    const transformKeys = loadSUT();
    const result = transformKeys([], [], { result: 42 });
    expect(result).toEqual({ result: 42 });
  });

  it('returns original result when no time aggregations are present', () => {
    const transformKeys = loadSUT();
    const result = transformKeys([{ type: 'value' }], [{ type: 'value' }], { result: 42 });
    expect(result).toEqual({ result: 42 });
  });

  it('transforms row keys using current user\'s timezone', () => {
    const transformKeys = loadSUT();

    CurrentUserStore.get.mockImplementationOnce(() => ({ timezone: 'Europe/Berlin' }));
    const input = [
      {
        source: 'leaf',
        key: ['2018-10-01T15:10:55.323Z'],
      },
      {
        source: 'leaf',
        key: ['2017-03-12T09:32:21.283-08:00'],
      },
    ];

    const result = transformKeys([{ type: 'time' }], [], input);

    expect(result).toEqual([
      {
        key: ['2018-10-01T17:10:55.323+02:00'],
        source: 'leaf',
      }, {
        key: ['2017-03-12T18:32:21.283+01:00'],
        source: 'leaf',
      }
    ]);
  });

  it('transforms column keys using current user\'s timezone', () => {
    const transformKeys = loadSUT();

    CurrentUserStore.get.mockImplementationOnce(() => ({ timezone: 'Europe/Berlin' }));
    const input = [
      {
        source: 'leaf',
        key: ['2018-10-01T15:10:55.323Z'],
      },
      {
        source: 'leaf',
        key: ['2017-03-12T09:32:21.283-08:00'],
      },
    ];

    const result = transformKeys([{ type: 'time' }], [], input);

    expect(result).toEqual([
      {
        key: ['2018-10-01T17:10:55.323+02:00'],
        source: 'leaf',
      }, {
        key: ['2017-03-12T18:32:21.283+01:00'],
        source: 'leaf',
      }
    ]);
  });

  it('transforms complete results using current user\'s timezone', () => {
    CurrentUserStore.get.mockImplementationOnce(() => ({ timezone: 'America/New_York' }));
    const { rowPivots, columnPivots, input, output } = fixtures.singleRowPivot;
    const transformKeys = loadSUT();
    const result = transformKeys(rowPivots, columnPivots, input);

    expect(result).toEqual(output);
  });

  it('does not transform complete results without time pivots', () => {
    const { rowPivots, columnPivots, input, output } = fixtures.noTimePivots;
    const transformKeys = loadSUT();
    const result = transformKeys(rowPivots, columnPivots, input);

    expect(result).toEqual(output);
  });
});