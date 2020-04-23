import NumberUtils from 'util/NumberUtils';
import createEvent from './CreateEvent';

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
    const event = createEvent('change');
    event.simulated = true;
    if (tracker) {
      tracker.setValue('');
    }
    input.dispatchEvent(event);
  },
};


export default FormUtils;
