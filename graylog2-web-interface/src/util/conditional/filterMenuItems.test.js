// @flow strict

import filterMenuItems, { filterCloudMenuItem } from './filterMenuItems';

describe('filterMenuItems', () => {
  it('should filter items by path', () => {
    const items = [
      { path: 'something', name: 'something' },
      { path: 'delete-me', name: 'delete me' },
    ];

    expect(filterMenuItems(items, ['delete-me'])).toEqual([
      { path: 'something', name: 'something' },
    ]);
  });

  it('should not filter items when specified', () => {
    const items = [
      { path: 'something', name: 'something' },
      { path: 'delete-me', name: 'delete me' },
    ];

    expect(filterMenuItems(items, ['delete-me'], false)).toEqual([
      { path: 'something', name: 'something' },
      { path: 'delete-me', name: 'delete me' },
    ]);
  });
});

describe('filterCloudMenuItem', () => {
  it('should not filter items by path when not on cloud', () => {
    const items = [
      { path: 'something', name: 'something' },
      { path: 'delete-me', name: 'delete me' },
    ];

    expect(filterCloudMenuItem(items, ['delete-me'])).toEqual([
      { path: 'something', name: 'something' },
      { path: 'delete-me', name: 'delete me' },
    ]);
  });

  it('should filter items by path when on cloud', () => {
    window.IS_CLOUD = true;

    const items = [
      { path: 'something', name: 'something' },
      { path: 'delete-me', name: 'delete me' },
    ];

    expect(filterCloudMenuItem(items, ['delete-me'])).toEqual([
      { path: 'something', name: 'something' },
    ]);

    window.IS_CLOUD = undefined;
  });
});
