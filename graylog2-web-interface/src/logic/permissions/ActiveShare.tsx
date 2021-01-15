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
import { $PropertyType } from 'utility-types';
import * as Immutable from 'immutable';

import type { ActiveShareType } from 'logic/permissions/types';

type InternalState = ActiveShareType;

export default class Grantee {
  _value: InternalState;

  constructor(
    grant: $PropertyType<ActiveShareType, 'grant'>,
    grantee: $PropertyType<ActiveShareType, 'grantee'>,
    capability: $PropertyType<ActiveShareType, 'capability'>,
  ) {
    this._value = { grant, grantee, capability };
  }

  get grant(): $PropertyType<InternalState, 'grant'> {
    return this._value.grant;
  }

  get grantee(): $PropertyType<InternalState, 'grantee'> {
    return this._value.grantee;
  }

  get capability(): $PropertyType<InternalState, 'capability'> {
    return this._value.capability;
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { grant, grantee, capability } = this._value;

    // eslint-disable-next-line no-use-before-define, @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map({ grant, grantee, capability }));
  }

  toJSON() {
    const { grant, grantee, capability } = this._value;

    return { grant, grantee, capability };
  }

  static fromJSON(value: ActiveShareType): Grantee {
    const { grant, grantee, capability } = value;

    return Grantee
      .builder()
      .grant(grant)
      .grantee(grantee)
      .capability(capability)
      .build();
  }

  static builder(): Builder {
    // eslint-disable-next-line no-use-before-define, @typescript-eslint/no-use-before-define
    return new Builder();
  }
}

type InternalBuilderState = Immutable.Map<string, any>;

class Builder {
  value: InternalBuilderState;

  constructor(value: InternalBuilderState = Immutable.Map()) {
    this.value = value;
  }

  grant(value: $PropertyType<InternalState, 'grant'>): Builder {
    return new Builder(this.value.set('grant', value));
  }

  grantee(value: $PropertyType<InternalState, 'grantee'>): Builder {
    return new Builder(this.value.set('grantee', value));
  }

  capability(value: $PropertyType<InternalState, 'capability'>): Builder {
    return new Builder(this.value.set('capability', value));
  }

  build(): Grantee {
    const { grant, grantee, capability } = this.value.toObject();

    return new Grantee(grant, grantee, capability);
  }
}
