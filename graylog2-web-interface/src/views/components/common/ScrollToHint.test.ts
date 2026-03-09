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
import { isElementCenterVisibleInContainer } from './ScrollToHint';

const mockElement = (top: number, height: number): HTMLElement =>
  ({ getBoundingClientRect: () => ({ top, height, bottom: top + height }) }) as unknown as HTMLElement;

const mockContainer = (top: number, bottom: number): HTMLElement =>
  ({ getBoundingClientRect: () => ({ top, bottom }) }) as unknown as HTMLElement;

describe('isElementCenterVisibleInContainer', () => {
  it('returns true when element center is inside the container', () => {
    const target = mockElement(100, 200); // center at 200
    const container = mockContainer(0, 500);

    expect(isElementCenterVisibleInContainer(target, container)).toBe(true);
  });

  it('returns false when element center is above the container', () => {
    const target = mockElement(-200, 100); // center at -150
    const container = mockContainer(0, 500);

    expect(isElementCenterVisibleInContainer(target, container)).toBe(false);
  });

  it('returns false when element center is below the container', () => {
    const target = mockElement(600, 100); // center at 650
    const container = mockContainer(0, 500);

    expect(isElementCenterVisibleInContainer(target, container)).toBe(false);
  });

  it('returns false when element is barely visible at the top (only 1px inside)', () => {
    // Element mostly above the container, with just 1px visible
    const target = mockElement(-99, 100); // center at -49
    const container = mockContainer(0, 500);

    expect(isElementCenterVisibleInContainer(target, container)).toBe(false);
  });

  it('returns false when element is barely visible at the bottom (only 1px inside)', () => {
    // Element mostly below the container, with just 1px visible
    const target = mockElement(499, 100); // center at 549
    const container = mockContainer(0, 500);

    expect(isElementCenterVisibleInContainer(target, container)).toBe(false);
  });

  it('returns true when element center is exactly at container top boundary', () => {
    const target = mockElement(-50, 100); // center at 0
    const container = mockContainer(0, 500);

    expect(isElementCenterVisibleInContainer(target, container)).toBe(true);
  });

  it('returns true when element center is exactly at container bottom boundary', () => {
    const target = mockElement(450, 100); // center at 500
    const container = mockContainer(0, 500);

    expect(isElementCenterVisibleInContainer(target, container)).toBe(true);
  });
});
