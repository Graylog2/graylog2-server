const ValidationsUtils = {
  setFieldValidity(fieldElement, condition, message) {
    // Return if browser does not support setCustomValidity
    if (typeof fieldElement.setCustomValidity !== 'function') {
      return;
    }

    fieldElement.setCustomValidity(condition ? message : '');
  },
};

export default ValidationsUtils;
