/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import { isElementVisibleInContainer } from './ScrollToHint';

describe('isElementVisibleInContainer', () => {
  const mockElement = (rect: Partial<DOMRect>): HTMLElement =>
    ({
      getBoundingClientRect: () => ({
        top: 0,
        bottom: 0,
        left: 0,
        right: 0,
        width: 0,
        height: 0,
        x: 0,
        y: 0,
        toJSON: () => {},
        ...rect,
      }),
    }) as unknown as HTMLElement;

  it('returns true when the target is fully visible inside the container', () => {
    const container = mockElement({ top: 0, bottom: 500 });
    const target = mockElement({ top: 100, bottom: 200 });

    expect(isElementVisibleInContainer(target, container)).toBe(true);
  });

  it('returns false when the target is below the container', () => {
    const container = mockElement({ top: 0, bottom: 500 });
    const target = mockElement({ top: 600, bottom: 700 });

    expect(isElementVisibleInContainer(target, container)).toBe(false);
  });

  it('returns false when the target is above the container', () => {
    const container = mockElement({ top: 100, bottom: 500 });
    const target = mockElement({ top: 0, bottom: 50 });

    expect(isElementVisibleInContainer(target, container)).toBe(false);
  });

  it('returns false when the target is partially visible at the bottom', () => {
    const container = mockElement({ top: 0, bottom: 500 });
    const target = mockElement({ top: 450, bottom: 600 });

    expect(isElementVisibleInContainer(target, container)).toBe(false);
  });

  it('returns false when the target is partially visible at the top', () => {
    const container = mockElement({ top: 100, bottom: 500 });
    const target = mockElement({ top: 50, bottom: 200 });

    expect(isElementVisibleInContainer(target, container)).toBe(false);
  });

  it('returns true when the target exactly matches the container bounds', () => {
    const container = mockElement({ top: 0, bottom: 500 });
    const target = mockElement({ top: 0, bottom: 500 });

    expect(isElementVisibleInContainer(target, container)).toBe(true);
  });
});
