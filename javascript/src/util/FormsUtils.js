import NumberUtils from 'util/NumberUtils';

const FormUtils = {
  getValueFromEventTarget(target) {
    switch (target.type) {
    case 'checkbox':
      return target.checked;
    case 'number':
      return (target.value === '' || !NumberUtils.isNumber(target.value) ? undefined : Number(target.value));
    default:
      return target.value;
    }
  },
};

export default FormUtils;
