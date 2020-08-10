// @flow strict
import * as Immutable from 'immutable';

import SharedEntity from 'logic/permissions/SharedEntity';

export type GRN = string;

export type CapabilityType = {|
  id: GRN,
  title: 'Viewer' | 'Manager' | 'Owner',
|};

export type GranteeType = {|
  id: GRN,
  title: string,
  type: 'global' | 'team' | 'user',
|};

export type ActiveShareType = {|
  grant: GRN,
  grantee: GRN,
  capability: GRN,
|};

export type SharedEntityType = {|
  id: GRN,
  owners: Array<GranteeType>,
  title: string,
  type: string,
|};

export type SharedEntities = Immutable.List<SharedEntity>;
