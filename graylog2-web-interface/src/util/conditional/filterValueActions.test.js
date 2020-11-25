// @flow strict

import filterValueActions, { filterCloudValueActions } from './filterValueActions';

describe('filterValueActions', () => {
  it('should filter items by type', () => {
    const items = [
      { type: 'something', name: 'something' },
      { type: 'delete-me', name: 'delete me' },
    ];

    expect(filterValueActions(items, ['delete-me'])).toEqual([
      { type: 'something', name: 'something' },
    ]);
  });
});

describe('filterCloudValueActions', () => {
  it('should not filter items by type when not on cloud', () => {
    const items = [
      { type: 'something', name: 'something' },
      { type: 'delete-me', name: 'delete me' },
    ];

    expect(filterCloudValueActions(items, ['delete-me'])).toEqual([
      { type: 'something', name: 'something' },
      { type: 'delete-me', name: 'delete me' },
    ]);
  });

  it('should filter items by type when on cloud', () => {
    window.IS_CLOUD = true;

    const items = [
      { type: 'something', name: 'something' },
      { type: 'delete-me', name: 'delete me' },
    ];

    expect(filterCloudValueActions(items, ['delete-me'])).toEqual([
      { type: 'something', name: 'something' },
    ]);

    window.IS_CLOUD = undefined;
  });
});
