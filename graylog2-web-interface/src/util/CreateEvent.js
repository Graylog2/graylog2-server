// @flow strict
// Workaround for IE11, see #7670
const createEvent = (type: string) => {
  if (typeof (Event) === 'function') {
    return new Event(type);
  }

  const event = document.createEvent('Event');
  event.initEvent(type, true, true);
  return event;
};

export default createEvent;
