// @flow strict
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
