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
import * as Immutable from 'immutable';
import { $PropertyType } from 'utility-types';

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

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
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

  static builder(): Builder {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder();
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
