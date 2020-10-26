// @flow strict

import isCloud from './isCloud';

describe('isCloud', () => {
  it('should return false when not in cloud', () => {
    expect(isCloud()).toBe(false);
  });

  it('should return true when in cloud through environment', () => {
    window.IS_CLOUD = true;

    expect(isCloud()).toBe(true);

    window.IS_CLOUD = undefined;
  });

  it('should return true when in cloud through config', () => {
    window.appConfig = { isCloud: true };

    expect(isCloud()).toBe(true);

    window.appConfig.isCloud = undefined;
  });
});
