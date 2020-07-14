import deduplicateValues from './DeduplicateValues';

describe('DeduplicateValues', () => {
  it('should not fail for empty rows', () => {
    expect(deduplicateValues([], [])).toEqual([]);
  });

  it('should deduplicate values', () => {
    const rows = [
      { controller: 'FooController', action: 'index' },
      { controller: 'FooController', action: 'create' },
      { controller: 'FooController', action: 'update' },
      { controller: 'FooController', action: 'delete' },
    ];
    const result = deduplicateValues(rows, ['controller', 'action']);

    expect(result).toEqual([
      { controller: 'FooController', action: 'index' },
      { action: 'create' },
      { action: 'update' },
      { action: 'delete' },
    ]);
  });

  it('should not deduplicate values for changing parent keys', () => {
    const rows = [
      { controller: 'FooController', action: 'index', method: 'GET' },
      { controller: 'FooController', action: 'index', method: 'POST' },
      { controller: 'BarController', action: 'index', method: 'GET' },
      { controller: 'BarController', action: 'index', method: 'POST' },
    ];
    const result = deduplicateValues(rows, ['controller', 'action', 'method']);

    expect(result).toEqual([
      { controller: 'FooController', action: 'index', method: 'GET' },
      { method: 'POST' },
      { controller: 'BarController', action: 'index', method: 'GET' },
      { method: 'POST' },
    ]);
  });

  it('should not deduplicate values for different parent keys', () => {
    const rows = [
      { controller: 'FooController', action: 'index', method: 'GET' },
      { controller: 'BarController', action: 'index', method: 'GET' },
    ];
    const result = deduplicateValues(rows, ['controller', 'action', 'method']);

    expect(result).toEqual([
      { controller: 'FooController', action: 'index', method: 'GET' },
      { controller: 'BarController', action: 'index', method: 'GET' },
    ]);
  });
});
