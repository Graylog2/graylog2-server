export default class ValueRefHelper {
  static VALUE_REF_VALUE_FIELD = '@value';

  static VALUE_REF_TYPE_FIELD = '@type';

  static VALUE_REF_PARAMETER_VALUE = 'parameter';

  static dataIsValueRef(data) {
    if (!data) {
      return false;
    }

    if (typeof data.has === 'function') {
      return data.size === 2 && data.has(ValueRefHelper.VALUE_REF_TYPE_FIELD) && data.has(ValueRefHelper.VALUE_REF_VALUE_FIELD);
    }

    const keys = Object.keys(data);

    return keys.length === 2 && keys.includes(ValueRefHelper.VALUE_REF_TYPE_FIELD)
      && keys.includes(ValueRefHelper.VALUE_REF_VALUE_FIELD);
  }

  static dataValueIsParameter(data) {
    if (!data) {
      return false;
    }

    if (typeof data.get === 'function') {
      return ValueRefHelper.dataIsValueRef(data) && data.get(ValueRefHelper.VALUE_REF_TYPE_FIELD) === ValueRefHelper.VALUE_REF_PARAMETER_VALUE;
    }

    return ValueRefHelper.dataIsValueRef(data) && data[ValueRefHelper.VALUE_REF_TYPE_FIELD] === ValueRefHelper.VALUE_REF_PARAMETER_VALUE;
  }

  static createValueRef(type, value) {
    return { [this.VALUE_REF_TYPE_FIELD]: type, [this.VALUE_REF_VALUE_FIELD]: value };
  }
}
