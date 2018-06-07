const ContentPackUtils = {
  convertToString(parameter) {
    switch (parameter.type) {
      case 'integer':
      case 'double':
        return parameter.default_value.toString();
      case 'boolean':
        return parameter.default_value ? 'true' : 'false';
      default:
        return parameter.default_value;
    }
  },

  convertValue(type, value) {
    switch (type) {
      case 'integer':
        return parseInt(value, 10);
      case 'double':
        return parseFloat(value);
      case 'boolean':
        return value === 'true';
      default:
        return value;
    }
  },
};

export default ContentPackUtils;
