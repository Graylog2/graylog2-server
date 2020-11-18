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
const DEFAULT_TIMEOUT = 1000 / 10; // 10 fps

/**
 * Use when an event handler should be called after a timeout, instead of every time the event is triggered.
 * This is specially useful for handling window resize callbacks that may be expensive.
 */
class EventHandlersThrottler {
  constructor() {
    this.eventMutex = null;
  }

  /**
   * @param eventHandler Callback actually handling the event
   * @param timeout (optional) Time to wait before calling the callback
   */
  throttle(eventHandler, timeout) {
    if (this.eventMutex) {
      return;
    }

    this.eventMutex = setTimeout(() => {
      this.eventMutex = null;
      eventHandler();
    }, timeout || DEFAULT_TIMEOUT);
  }
}

export default EventHandlersThrottler;
