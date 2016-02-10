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
