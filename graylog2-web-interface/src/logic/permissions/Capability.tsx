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
// @flow strict
import * as Immutable from 'immutable';
import { $PropertyType } from 'utility-types';

import type { CapabilityType } from 'logic/permissions/types';

type InternalState = CapabilityType;

export default class Capability {
  _value: InternalState;

  constructor(
    id: $PropertyType<InternalState, 'id'>,
    title: $PropertyType<InternalState, 'title'>,
  ) {
    this._value = { id, title };
  }

  get id(): $PropertyType<InternalState, 'id'> {
    return this._value.id;
  }

  get title(): $PropertyType<InternalState, 'title'> {
    return this._value.title;
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { id, title } = this._value;

    // eslint-disable-next-line no-use-before-define,@typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map({ id, title }));
  }

  toJSON() {
    const { id, title } = this._value;

    return { id, title };
  }

  static fromJSON(value: InternalState): Capability {
    const { id, title } = value;

    return Capability
      .builder()
      .id(id)
      .title(title)
      .build();
  }

  // eslint-disable-next-line no-use-before-define
  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define,@typescript-eslint/no-use-before-define
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

  build(): Capability {
    const { id, title } = this.value.toObject();

    return new Capability(id, title);
  }
}
