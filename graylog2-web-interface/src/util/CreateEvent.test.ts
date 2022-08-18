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
import createEvent from './CreateEvent';

describe('CreateEvent', () => {
  it('returns event if `Event` constructor is available', () => {
    const event = createEvent('submit');

    expect(event).not.toBeNull();
    expect(event).toBeInstanceOf(Event);
  });

  it('returns event if `Event` constructor is not available', () => {
    const oldEvent: typeof Event = window.Event;

    window.Event = null;

    const event = createEvent('submit');

    expect(event).not.toBeNull();
    expect(event).toBeInstanceOf(oldEvent);

    window.Event = oldEvent;
  });
});
