// @flow strict

import * as Immutable from 'immutable';

import { getAuthServicePlugin } from 'util/AuthenticationService';

type InternalState = {|
  id: string,
  title: string,
  description: string,
  config: {
    type: string,
    ...
  },
|};

export type AuthenticationBackendJson = {|
  id: string,
  title: string,
  description: string,
  config: {
    type: string,
    ...
  },
|};

const configFromJson = (config: $PropertyType<AuthenticationBackendJson, 'config'>) => {
  const authService = getAuthServicePlugin(config.type, true);

  if (authService && typeof authService.configFromJson === 'function') {
    return authService.configFromJson(config);
  }

  return config;
};

const configToJson = (config: $PropertyType<AuthenticationBackendJson, 'config'>) => {
  const authService = getAuthServicePlugin(config.type, true);

  if (authService && typeof authService.configToJson === 'function') {
    return authService.configToJson(config);
  }

  return config;
};

export default class AuthenticationBackend {
  _value: InternalState;

  constructor(
    id: $PropertyType<InternalState, 'id'>,
    title: $PropertyType<InternalState, 'title'>,
    description: $PropertyType<InternalState, 'description'>,
    config: $PropertyType<InternalState, 'config'>,
  ) {
    this._value = {
      id,
      title,
      description,
      config,
    };
  }

  get id(): $PropertyType<InternalState, 'id'> {
    return this._value.id;
  }

  get title(): $PropertyType<InternalState, 'title'> {
    return this._value.title;
  }

  get description(): $PropertyType<InternalState, 'description'> {
    return this._value.description;
  }

  get config(): $PropertyType<InternalState, 'config'> {
    return this._value.config;
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const {
      id,
      title,
      description,
      config,
    } = this._value;

    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({
      id,
      title,
      description,
      config,
    }));
  }

  toJSON() {
    const {
      id,
      title,
      description,
      config,
    } = this._value;

    const formattedConfig = configToJson(config);

    return {
      id,
      title,
      description,
      config: formattedConfig,
    };
  }

  static fromJSON(value: AuthenticationBackendJson) {
    /* eslint-disable camelcase */
    const {
      id,
      title,
      description,
      config,
    } = value;

    const formattedConfig = configFromJson(config);

    /* eslint-enable camelcase */
    return new AuthenticationBackend(
      id,
      title,
      description,
      formattedConfig,
    );
  }

  // eslint-disable-next-line no-use-before-define
  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define
    return new Builder();
  }
}

type InternalBuilderState = Immutable.Map<string, any>;

class Builder {
  value: InternalBuilderState;

  constructor(value: InternalBuilderState = Immutable.Map()) {
    this.value = value;
  }

  id(value: $PropertyType<InternalState, 'id'>): Builder {
    return new Builder(this.value.set('id', value));
  }

  title(value: $PropertyType<InternalState, 'title'>): Builder {
    return new Builder(this.value.set('title', value));
  }

  description(value: $PropertyType<InternalState, 'description'>): Builder {
    return new Builder(this.value.set('description', value));
  }

  config(value: $PropertyType<InternalState, 'config'>): Builder {
    return new Builder(this.value.set('config', value));
  }

  build(): AuthenticationBackend {
    const {
      id,
      title,
      description,
      config,
    } = this.value.toObject();

    return new AuthenticationBackend(
      id,
      title,
      description,
      config,
    );
  }
}
