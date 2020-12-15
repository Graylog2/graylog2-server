/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
