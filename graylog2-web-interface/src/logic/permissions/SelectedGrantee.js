// @flow strict
import type { Grantee as GranteeType } from 'logic/permissions/types';

import { type GranteeInterface } from './GranteeInterface';
import Grantee from './Grantee';
import Role from './Role';

type InternalState = {
  id: $PropertyType<Grantee, 'id'>,
  title: $PropertyType<Grantee, 'title'>,
  type: $PropertyType<Grantee, 'type'>,
  roleId: $PropertyType<Role, 'id'>,
};

export default class SelectedGrantee implements GranteeInterface {
  _value: InternalState;

  constructor(
    id: $PropertyType<InternalState, 'id'>,
    title: $PropertyType<InternalState, 'title'>,
    type: $PropertyType<InternalState, 'type'>,
    roleId: $PropertyType<InternalState, 'roleId'>,
  ) {
    this._value = { id, title, type, roleId };
  }

  get id(): $PropertyType<InternalState, 'id'> {
    return this._value.id;
  }

  get title(): $PropertyType<InternalState, 'title'> {
    return this._value.title;
  }

  get type(): $PropertyType<InternalState, 'type'> {
    return this._value.type;
  }

  get roleId(): $PropertyType<InternalState, 'roleId'> {
    return this._value.roleId;
  }

  static create(
    id: $PropertyType<InternalState, 'id'>,
    title: $PropertyType<InternalState, 'title'>,
    type: $PropertyType<InternalState, 'type'>,
    roleId: $PropertyType<InternalState, 'roleId'>,
  ) {
    return new SelectedGrantee(id, title, type, roleId);
  }
}
