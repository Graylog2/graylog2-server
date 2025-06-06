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
import { singleton } from 'logic/singleton';
import standardMapper from 'logic/permissions/StandardEntityPermissionsMapper';

export interface EntityPermissionsMapper {
  mapForIdAndType(id: string, type: string): string | undefined;
  mapForType(type: string): string | undefined;
}

class EntityPermissionsMapperBinder {
  static __registrations: Array<EntityPermissionsMapper> = [standardMapper];

  static register(implementingClass: EntityPermissionsMapper) {
    this.__registrations.push(implementingClass);
  }

  static mapToPrefix(type: string, id: string): string | undefined {
    const mapForIdAndType = this.__registrations
      .map((mapper) => mapper.mapForIdAndType(id, type))
      .find((prefix) => prefix !== undefined);

    return (
      mapForIdAndType ||
      this.__registrations.map((mapper) => mapper.mapForType(type)).find((prefix) => prefix !== undefined)
    );
  }
}

const SingletonEntityPermissionsMapperBinder = singleton(
  'logic.entitypermissionsmapper.binder',
  () => EntityPermissionsMapperBinder,
);
// eslint-disable-next-line @typescript-eslint/no-redeclare
type SingletonEntityPermissionsMapperBinder = InstanceType<typeof EntityPermissionsMapperBinder>;

export default SingletonEntityPermissionsMapperBinder;
