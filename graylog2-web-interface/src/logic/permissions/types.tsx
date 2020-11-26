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

import SharedEntity from 'logic/permissions/SharedEntity';

export type GRN = string;

export type CapabilityType = {
  id: GRN,
  title: 'Viewer' | 'Manager' | 'Owner',
};

export type GranteeType = {
  id: GRN,
  title: string,
  type: 'global' | 'team' | 'user' | 'error',
};

export type ActiveShareType = {
  grant: GRN,
  grantee: GRN,
  capability: GRN,
};

export type SharedEntityType = {
  id: GRN,
  owners: Array<GranteeType>,
  title: string,
  type: string,
};

export type SharedEntities = Immutable.List<SharedEntity>;
