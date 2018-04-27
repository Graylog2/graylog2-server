import normalizeRows from './NormalizeRows';

describe('NormalizeRows', () => {
  it('should normalize empty array', () => {
    const data = [];
    const result = normalizeRows(['controller'], [], ['count'], data);
    expect(result).toHaveLength(0);
    expect(result).toEqual([]);
  });
  it('should normalize flat array', () => {
    const data = [{ controller: 'PostsController', count: 3232 }, { controller: 'UsersController', count: 3288 }, { controller: 'LoginController', count: 218 }];
    const result = normalizeRows(['controller'], [], ['count'], data);
    expect(result).toHaveLength(3);
    expect(result).toEqual(data);
  });
  it('should normalize flat array without fields', () => {
    const data = [{ controller: 'PostsController', count: 3232 }, { controller: 'UsersController', count: 3288 }, { controller: 'LoginController', count: 218 }];
    const result = normalizeRows([], ['count'], [], data);
    expect(result).toHaveLength(0);
    expect(result).toEqual([]);
  });
  it('should normalize flat array without series', () => {
    const data = [{ controller: 'PostsController', count: 3232 }, { controller: 'UsersController', count: 3288 }, { controller: 'LoginController', count: 218 }];
    const result = normalizeRows(['controller'], [], [], data);
    const expected = [{ controller: 'PostsController' }, { controller: 'UsersController' }, { controller: 'LoginController' }];
    expect(result).toHaveLength(3);
    expect(result).toEqual(expected);
  });
  it('should normalize nested array', () => {
    const data = [
      { controller: 'PostsController', action: [{ action: 'index', count: 232 }, { action: 'edit', count: 381 }], count: 3232 },
      { controller: 'UsersController', action: [{ action: 'index', count: 876 }, { action: 'edit', count: 564 }], count: 3288 },
      { controller: 'LoginController', action: [{ action: 'index', count: 423 }, { action: 'edit', count: 168 }], count: 218 },
    ];
    const expected = [
      { controller: 'PostsController', action: 'index', count: 232 },
      { controller: 'PostsController', action: 'edit', count: 381 },
      { controller: 'UsersController', action: 'index', count: 876 },
      { controller: 'UsersController', action: 'edit', count: 564 },
      { controller: 'LoginController', action: 'index', count: 423 },
      { controller: 'LoginController', action: 'edit', count: 168 },
    ];
    const result = normalizeRows(['controller', 'action'], [], ['count'], data);
    expect(result).toHaveLength(3 * 2);
    expect(result).toEqual(expected);
  });
  it('should normalize nested array with missing series', () => {
    const data = [
      { controller: 'PostsController', action: [{ action: 'index', count: 232 }, { action: 'edit', count: 381 }], count: 3232 },
      { controller: 'UsersController', action: [{ action: 'index', count: 876 }, { action: 'edit', count: 564 }], count: 3288 },
      { controller: 'LoginController', action: [{ action: 'index', count: 423 }, { action: 'edit', count: 168 }], count: 218 },
    ];
    const expected = [
      { controller: 'PostsController', action: 'index', count: 232 },
      { controller: 'PostsController', action: 'edit', count: 381 },
      { controller: 'UsersController', action: 'index', count: 876 },
      { controller: 'UsersController', action: 'edit', count: 564 },
      { controller: 'LoginController', action: 'index', count: 423 },
      { controller: 'LoginController', action: 'edit', count: 168 },
    ];
    const result = normalizeRows(['controller', 'action'], [], ['count', 'sum(took_ms)'], data);
    expect(result).toHaveLength(3 * 2);
    expect(result).toEqual(expected);
  });
  it('should normalize nested array with multiple series', () => {
    const data = [
      { controller: 'PostsController', action: [{ action: 'index', count: 232, 'sum(took_ms)': 983128 }, { action: 'edit', count: 381, 'sum(took_ms)': 55677 }], count: 3232 },
      { controller: 'UsersController', action: [{ action: 'index', count: 876, 'sum(took_ms)': 312038 }, { action: 'edit', count: 564, 'sum(took_ms)': 75472 }], count: 3288 },
      { controller: 'LoginController', action: [{ action: 'index', count: 423, 'sum(took_ms)': 28283 }, { action: 'edit', count: 168, 'sum(took_ms)': 3828 }], count: 218 },
    ];
    const expected = [
      { controller: 'PostsController', action: 'index', count: 232, 'sum(took_ms)': 983128 },
      { controller: 'PostsController', action: 'edit', count: 381, 'sum(took_ms)': 55677 },
      { controller: 'UsersController', action: 'index', count: 876, 'sum(took_ms)': 312038 },
      { controller: 'UsersController', action: 'edit', count: 564, 'sum(took_ms)': 75472 },
      { controller: 'LoginController', action: 'index', count: 423, 'sum(took_ms)': 28283 },
      { controller: 'LoginController', action: 'edit', count: 168, 'sum(took_ms)': 3828 },
    ];
    const result = normalizeRows(['controller', 'action'], [], ['count', 'sum(took_ms)'], data);
    expect(result).toHaveLength(3 * 2);
    expect(result).toEqual(expected);
  });
  it('should return empty list if one field is missing', () => {
    const data = [
      { controller: 'PostsController', action: [{ action: 'index', count: 232, 'sum(took_ms)': 983128 }, { action: 'edit', count: 381, 'sum(took_ms)': 55677 }], count: 3232 },
      { controller: 'UsersController', action: [{ action: 'index', count: 876, 'sum(took_ms)': 312038 }, { action: 'edit', count: 564, 'sum(took_ms)': 75472 }], count: 3288 },
      { controller: 'LoginController', action: [{ action: 'index', count: 423, 'sum(took_ms)': 28283 }, { action: 'edit', count: 168, 'sum(took_ms)': 3828 }], count: 218 },
    ];
    const result = normalizeRows(['controller', 'missing'], [], ['count', 'sum(took_ms)'], data);
    expect(result).toHaveLength(0);
    expect(result).toEqual([]);
  });
});
