// @flow strict

type GRN = string;

export type Role = {|
  id: GRN,
  title: 'Viewer' | 'Manager' | 'Owner',
|};

export type Grantee = {|
  id: GRN,
  type: 'global' | 'team' | 'user',
  title: string,
|};

export type ActiveShare = {|
  grant: GRN,
  grantee: GRN,
  role: GRN,
|};

export type MissingDependency = {|
  id: GRN,
  title: String,
  owners: Array<$PropertyType<Grantee, 'id'>>,
|};

export type SelectedGranteeRoles = {|
  [grantee: $PropertyType<Grantee, 'id'>]: $PropertyType<Role, 'id'>,
|};

export type EntityShareResponse = {|
  entity: GRN,
  available_grantees: Array<Grantee>,
  available_roles: Array<Role>,
  active_shares: Array<ActiveShare>,
  selected_grantee_roles: SelectedGranteeRoles,
  missing_dependencies: Array<MissingDependency>,
|};

export type EntityShareRequest = {|
  selected_grantee_roles?: SelectedGranteeRoles,
|};
