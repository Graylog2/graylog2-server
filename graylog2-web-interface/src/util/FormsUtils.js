import NumberUtils from 'util/NumberUtils';

const FormUtils = {
  getValueFromInput(input) {
    switch (input.type) {
      case 'radio':
        return (input.value === 'true' || input.value === 'false' ? input.value === 'true' : input.value);
      case 'checkbox':
        return input.checked;
      case 'number':
        return (input.value === '' || !NumberUtils.isNumber(input.value) ? undefined : Number(input.value));
      default:
        return input.value;
    }
  },
  triggerInput(urlInput) {
    const { input } = urlInput;
    const tracker = input._valueTracker;
    const event = this.createEvent('change');
    event.simulated = true;
    if (tracker) {
      tracker.setValue('');
    }
    input.dispatchEvent(event);
  },
  // Workaround for IE11, see #7670
  createEvent(type) {
    if (typeof (Event) === 'function') {
      return new Event(type);
    }

    const event = document.createEvent('Event');
    event.initEvent(type, true, true);
    return event;
  },
};


export default FormUtils;
