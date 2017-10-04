import NumberUtils from 'util/NumberUtils';

const FormUtils = {
  getValueFromInput(input) {
    switch (input.type) {
      case 'radio':
        const value = input.value;
        return (value === 'true' || value === 'false' ? value === 'true' : value);
      case 'checkbox':
        return input.checked;
      case 'number':
        return (input.value === '' || !NumberUtils.isNumber(input.value) ? undefined : Number(input.value));
      default:
        return input.value;
    }
  },

  // Emulates a minimal input event that can be used with FormUtils#getValueFromInput()
  inputEvent(name, value) {
    return { target: { name: name, value: value } };
  },
};

export default FormUtils;
