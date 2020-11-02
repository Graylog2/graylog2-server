// @flow strict
import * as Immutable from 'immutable';

type InternalState = {
  enabled: boolean,
  usernameHeader: string,
};

export type HTTPHeaderAuthConfigJSON = {
  enabled: boolean,
  username_header: string,
};

export default class HTTPHeaderAuthConfig {
  _value: InternalState;

  constructor(
    usernameHeader: $PropertyType<InternalState, 'usernameHeader'>,
    enabled: $PropertyType<InternalState, 'enabled'>,
  ) {
    this._value = { usernameHeader, enabled };
  }

  get usernameHeader() {
    return this._value.usernameHeader;
  }

  get enabled() {
    return this._value.enabled;
  }

  toBuilder() {
    const {
      usernameHeader,
      enabled,
    } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ usernameHeader, enabled }));
  }

  static create(
    usernameHeader: $PropertyType<InternalState, 'usernameHeader'>,
    enabled: $PropertyType<InternalState, 'enabled'>,
  ) {
    return new HTTPHeaderAuthConfig(usernameHeader, enabled);
  }

  toJSON() {
    const { usernameHeader, enabled } = this._value;

    return {
      username_header: usernameHeader,
      enabled,
    };
  }

  static fromJSON(value: HTTPHeaderAuthConfigJSON) {
    const { username_header: usernameHeader, enabled } = value;

    return HTTPHeaderAuthConfig.create(usernameHeader, enabled);
  }
}

type BuilderState = Immutable.Map<string, any>;
class Builder {
  value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  usernameHeader(value: $PropertyType<InternalState, 'usernameHeader'>) {
    return new Builder(this.value.set('usernameHeader', value));
  }

  enabled(value: $PropertyType<InternalState, 'enabled'>) {
    return new Builder(this.value.set('enabled', value));
  }

  build() {
    const { usernameHeader, enabled } = this.value.toObject();

    return new HTTPHeaderAuthConfig(usernameHeader, enabled);
  }
}
