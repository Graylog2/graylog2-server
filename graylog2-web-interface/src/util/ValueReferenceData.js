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
import Immutable, { Map, List } from 'immutable';

import ValueRefHelper from './ValueRefHelper';

/**
 * A path object that can be used to obtain information about the data at a path and also modify it.
 */
class Path {
  constructor(path, valueIsReference, valueIsParameter, getter, setter, parameterSetter, valueTypeGetter) {
    this.path = path;
    this.valueIsReference = valueIsReference;
    this.valueIsParameter = valueIsParameter;
    this.getter = getter;
    this.setter = setter;
    this.parameterSetter = parameterSetter;
    this.valueTypeGetter = valueTypeGetter;
  }

  /**
   * Get the concatenated value path as a string. (e.g. "stream_rules.0.title")
   * @returns the path as string
   */
  getPath() {
    return this.path;
  }

  /**
   * Get the value for the path.
   * @returns the value
   */
  getValue() {
    return this.getter();
  }

  /**
   * Sets the value for the path.
   * @param the value to set
   */
  setValue(value) {
    this.setter(value);
  }

  /**
   * Set the value for the path to a parameter with the given name.
   * This will throw an Error if the value at the path is not a value-reference! Consumers should use #isValueParameter
   * before trying to use this.
   * @param name the parameter name
   */
  setParameter(name) {
    this.parameterSetter(name);
  }

  /**
   * Gets the type of the value.
   * @returns the type string
   */
  getValueType() {
    return this.valueTypeGetter();
  }

  /**
   * Returns true if the path value is a value-reference.
   * @returns true if value is value-reference, false otherwise
   */
  isValueRef() {
    return this.valueIsReference;
  }

  /**
   * Returns true if the path value is a parameter.
   * @returns true if value is a parameter, false otherwise
   */
  isValueParameter() {
    return this.valueIsParameter;
  }
}

/**
 * Wraps content pack entity data and generates paths into the nested data so we can show a flat list of values for
 * content pack entities.
 */
export default class ValueReferenceData {
  constructor(data) {
    this.data = Map(Immutable.fromJS(data));
    this.paths = Map();
    this.walkPaths();
  }

  /**
   * Get the path object which contains the flat paths into the nested data. The value of each path key is a Path
   * object that can be used to obtain information about the value for the path and also modify it.
   * @returns the paths object
   */
  getPaths() {
    return this.paths.toJS();
  }

  /**
   * Get the actual data back. This can be used to get the updated data after modifying it.
   * @returns the data
   */
  getData() {
    return this.data.toJS();
  }

  /**
   * @private
   */
  walkPaths(parentPath = []) {
    const data = parentPath.length > 0 ? this.data.getIn(parentPath) : this.data;

    if (Map.isMap(data)) {
      if (ValueRefHelper.dataIsValueRef(data)) {
        // We handle ValueReference data objects as leaf nodes
        this.addPath(parentPath);
      } else {
        data.keySeq().toJS().forEach((key) => {
          this.walkPaths(parentPath.concat([key]));
        });
      }
    } else if (List.isList(data)) {
      data.toArray().forEach((value, idx) => {
        this.walkPaths(parentPath.concat([idx]));
      });
    } else {
      // We arrived at the leaf of the tree so create a Path object for the current path
      this.addPath(parentPath);
    }
  }

  /**
   * @private
   */
  addPath(path) {
    const stringPath = path.join('.');
    const leaf = new Path(
      stringPath,
      ValueRefHelper.dataIsValueRef(this.data.getIn(path)),
      ValueRefHelper.dataValueIsParameter(this.data.getIn(path)),
      this.pathGetter(path),
      this.pathSetter(path),
      this.pathParameterSetter(path),
      this.pathType(path),
    );

    this.paths = this.paths.set(stringPath, leaf);
  }

  /**
   * Data getter that knows about value-reference and regular data.
   * @private
   */
  pathGetter(path) {
    return () => {
      if (ValueRefHelper.dataIsValueRef(this.data.getIn(path))) {
        return this.data.getIn(path.concat(ValueRefHelper.VALUE_REF_VALUE_FIELD));
      }

      return this.data.getIn(path);
    };
  }

  /**
   * Data setter that knows about value-reference and regular data.
   * @private
   */
  pathSetter(path) {
    return (value) => {
      if (ValueRefHelper.dataIsValueRef(this.data.getIn(path))) {
        this.data = this.data.setIn(path.concat(ValueRefHelper.VALUE_REF_VALUE_FIELD), value);
      } else {
        this.data = this.data.setIn(path, value);
      }
    };
  }

  /**
   * Converts the value for the path to a parameter. If the value for the path isn't a value-reference, it throws
   * and error.
   * @private
   */
  pathParameterSetter(path) {
    return (name) => {
      if (ValueRefHelper.dataIsValueRef(this.data.getIn(path))) {
        this.data = this.data.setIn(path, Map({
          [ValueRefHelper.VALUE_REF_VALUE_FIELD]: name,
          [ValueRefHelper.VALUE_REF_TYPE_FIELD]: ValueRefHelper.VALUE_REF_PARAMETER_VALUE,
        }));
      } else {
        throw new Error(`Cannot set parameter on non-value-reference field: ${path.join('.')}`);
      }
    };
  }

  /**
   * Gets the data type for a path value. It can handle value-reference and regular data values.
   * @private
   */
  pathType(path) {
    return () => {
      const data = this.data.getIn(path);

      if (ValueRefHelper.dataIsValueRef(data)) {
        return this.data.getIn(path.concat(ValueRefHelper.VALUE_REF_TYPE_FIELD));
      }

      return (typeof data);
    };
  }
}
