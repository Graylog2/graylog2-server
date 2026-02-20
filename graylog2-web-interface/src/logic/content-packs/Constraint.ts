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
import Immutable from 'immutable';

type ConstraintValue = {
  type: string;
  plugin: string;
  version: string;
};

type ConstraintJSON = {
  type: string;
  version: string;
  plugin?: string;
};

export default class Constraint {
  _value: ConstraintValue;

  constructor(type: string, version: string, plugin: string = 'server') {
    this._value = { type, plugin, version };
  }

  get type(): string {
    return this._value.type;
  }

  get plugin(): string {
    return this._value.plugin;
  }

  get version(): string {
    return this._value.version;
  }

  toBuilder(): Builder {
    const { type, plugin, version } = this._value;

    return new Builder(Immutable.Map({ type, plugin, version }));
  }

  static create(type: string, version: string, plugin: string = 'server'): Constraint {
    return new Constraint(type, version, plugin);
  }

  toJSON(): ConstraintJSON {
    const { type, plugin, version } = this._value;

    if (plugin === 'server') {
      return {
        type,
        version,
      };
    }

    return {
      type,
      plugin,
      version,
    };
  }

  equals(other: Constraint | { version: string; type: string; plugin: string }): boolean {
    if (!other.version || !other.plugin || !other.type) {
      return false;
    }

    return other.version === this.version && other.type === this.type && other.plugin === this.plugin;
  }

  static fromJSON(value: ConstraintJSON): Constraint {
    const { type, version, plugin } = value;

    return Constraint.create(type, version, plugin);
  }

  static builder(): Builder {
    return new Builder().plugin('server');
  }
}

class Builder {
  value: Immutable.Map<string, string>;

  constructor(value: Immutable.Map<string, string> = Immutable.Map()) {
    this.value = value;
  }

  type(value: string): Builder {
    return new Builder(this.value.set('type', value));
  }

  plugin(value: string): Builder {
    return new Builder(this.value.set('plugin', value));
  }

  version(value: string): Builder {
    return new Builder(this.value.set('version', value));
  }

  build(): Constraint {
    const { type, plugin, version } = this.value.toObject() as ConstraintValue;

    return new Constraint(type, version, plugin);
  }
}
