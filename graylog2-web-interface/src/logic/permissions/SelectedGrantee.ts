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
import type { GranteeInterface } from './GranteeInterface';
import type Grantee from './Grantee';
import type Capability from './Capability';
import type { ActiveShares } from './EntityShareState';

type InternalState = {
  id: Grantee['id'];
  title: Grantee['title'];
  type: Grantee['type'];
  capabilityId: Capability['id'];
};

export type CurrentState = 'new' | 'changed' | 'unchanged';

export default class SelectedGrantee implements GranteeInterface {
  _value: InternalState;

  constructor(
    id: InternalState['id'],
    title: InternalState['title'],
    type: InternalState['type'],
    capabilityId: InternalState['capabilityId'],
  ) {
    this._value = { id, title, type, capabilityId };
  }

  get id(): InternalState['id'] {
    return this._value.id;
  }

  get title(): InternalState['title'] {
    return this._value.title;
  }

  get type(): InternalState['type'] {
    return this._value.type;
  }

  get capabilityId(): InternalState['capabilityId'] {
    return this._value.capabilityId;
  }

  currentState(activeShares: ActiveShares): CurrentState {
    const { capabilityId, id } = this._value;
    const activeShare = activeShares.find((share) => share.grantee === id);

    if (!activeShare) return 'new';
    if (activeShare.capability !== capabilityId) return 'changed';

    return 'unchanged';
  }

  static create(
    id: InternalState['id'],
    title: InternalState['title'],
    type: InternalState['type'],
    capabilityId: InternalState['capabilityId'],
  ) {
    return new SelectedGrantee(id, title, type, capabilityId);
  }
}
