import View from 'views/logic/views/View';

import ViewTitle from './ViewTitle';

describe('ViewTitle', () => {
  it('returns default title for unsaved search', () => {
    expect(ViewTitle(undefined, View.Type.Search)).toBe('Unsaved Search');
  });

  it('returns default title for unsaved dashboard', () => {
    expect(ViewTitle(undefined, View.Type.Dashboard)).toBe('Unsaved Dashboard');
  });

  it('returns actual title if present', () => {
    expect(ViewTitle('My title', undefined)).toBe('My title');
  });
});
