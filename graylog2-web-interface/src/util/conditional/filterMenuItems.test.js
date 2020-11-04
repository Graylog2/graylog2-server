// @flow strict

import filterMenuItems, { filterCloudMenuItems } from './filterMenuItems';

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
});

describe('filterCloudMenuItem', () => {
  it('should not filter items by path when not on cloud', () => {
    const items = [
      { path: 'something', name: 'something' },
      { path: 'delete-me', name: 'delete me' },
    ];

    expect(filterCloudMenuItems(items, ['delete-me'])).toEqual([
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

    expect(filterCloudMenuItems(items, ['delete-me'])).toEqual([
      { path: 'something', name: 'something' },
    ]);

    window.IS_CLOUD = undefined;
  });
});
