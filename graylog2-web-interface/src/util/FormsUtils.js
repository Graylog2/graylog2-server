import NumberUtils from 'util/NumberUtils';

const FormUtils = {
  getValueFromInput(input) {
    switch (input.type) {
      case 'radio':
        const { value } = input;
        return (value === 'true' || value === 'false' ? value === 'true' : value);
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
    const event = new Event('change', { bubbles: true });
    event.simulated = true;
    if (tracker) {
      tracker.setValue('');
    }
    input.dispatchEvent(event);
  },
};

export default FormUtils;
