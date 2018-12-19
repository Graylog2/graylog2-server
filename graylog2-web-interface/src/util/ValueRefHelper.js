export default class ValueRefHelper {
  static VALUE_REF_VALUE_FIELD = '@value';
  static VALUE_REF_TYPE_FIELD = '@type';
  static VALUE_REF_PARAMETER_VALUE = 'parameter';

  static dataIsValueRef(data) {
    return data.size === 2 && data.has(ValueRefHelper.VALUE_REF_TYPE_FIELD) && data.has(ValueRefHelper.VALUE_REF_VALUE_FIELD);
  }

  static dataValueIsParameter(data) {
    return ValueRefHelper.dataIsValueRef(data) && data.get(ValueRefHelper.VALUE_REF_TYPE_FIELD) === ValueRefHelper.VALUE_REF_PARAMETER_VALUE;
  }
}
