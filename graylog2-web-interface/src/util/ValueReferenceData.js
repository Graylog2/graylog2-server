import Immutable, { Map, List } from 'immutable';

const VALUE_REF_VALUE_FIELD = 'value';
const VALUE_REF_TYPE_FIELD = 'type';
const VALUE_REF_PARAMETER_VALUE = 'parameter';

const dataIsValueRef = (data) => {
  return data.size === 2 && data.has(VALUE_REF_TYPE_FIELD) && data.has(VALUE_REF_VALUE_FIELD);
};

const dataValueIsParameter = (data) => {
  return dataIsValueRef(data) && data.get(VALUE_REF_TYPE_FIELD) === VALUE_REF_PARAMETER_VALUE;
};

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

  getPath() {
    return this.path;
  }

  getValue() {
    return this.getter();
  }

  setValue(value) {
    this.setter(value);
  }

  setParameter(name) {
    this.parameterSetter(name);
  }

  getValueType() {
    return this.valueTypeGetter();
  }

  isValueRef() {
    return this.valueIsReference;
  }

  isValueParameter() {
    return this.valueIsParameter;
  }
}

export default class ValueReferenceData {
  constructor(data) {
    this.data = Map(Immutable.fromJS(data));
    this.paths = Map();
    this.walkPaths();
  }

  getPaths() {
    return this.paths.toJS();
  }

  getData() {
    return this.data.toJS();
  }

  walkPaths(parentPath = []) {
    const data = parentPath.length > 0 ? this.data.getIn(parentPath) : this.data;

    if (Map.isMap(data)) {
      if (dataIsValueRef(data)) {
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
      this.addPath(parentPath);
    }
  }

  addPath(path) {
    const stringPath = path.join('.');
    const leaf = new Path(
      stringPath,
      dataIsValueRef(this.data.getIn(path)),
      dataValueIsParameter(this.data.getIn(path)),
      this.pathGetter(path),
      this.pathSetter(path),
      this.pathParameterSetter(path),
      this.pathType(path));
    this.paths = this.paths.set(stringPath, leaf);
  }

  pathGetter(path) {
    return () => {
      if (dataIsValueRef(this.data.getIn(path))) {
        return this.data.getIn(path.concat(VALUE_REF_VALUE_FIELD));
      } else {
        return this.data.getIn(path);
      }
    };
  }

  pathSetter(path) {
    return (value) => {
      if (dataIsValueRef(this.data.getIn(path))) {
        this.data = this.data.setIn(path.concat(VALUE_REF_VALUE_FIELD), value);
      } else {
        this.data = this.data.setIn(path, value);
      }
    };
  }

  pathParameterSetter(path) {
    return (name) => {
      if (dataIsValueRef(this.data.getIn(path))) {
        this.data = this.data.setIn(path, Map({ [VALUE_REF_VALUE_FIELD]: name, [VALUE_REF_TYPE_FIELD]: VALUE_REF_PARAMETER_VALUE }));
      } else {
        throw new Error(`Cannot set parameter on non-value-reference field: ${path.join('.')}`);
      }
    };
  }

  pathType(path) {
    return () => {
      const data = this.data.getIn(path);

      if (dataIsValueRef(data)) {
        return this.data.getIn(path.concat(VALUE_REF_TYPE_FIELD));
      } else {
        return (typeof data);
      }
    };
  }
}
