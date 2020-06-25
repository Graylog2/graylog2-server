// @flow strict

export type GRN = string;

export type Role = {|
  id: GRN,
  title: 'Viewer' | 'Manager' | 'Owner',
|};

export type Grantee = {|
  id: GRN,
  title: string,
  type: 'global' | 'team' | 'user',
|};

export type ActiveShare = {|
  grant: GRN,
  grantee: GRN,
  role: GRN,
|};

export type MissingDependency = {|
  id: GRN,
  owners: Array<$PropertyType<Grantee, 'id'>>,
  title: String,
|};

export type SelectedGranteeRoles = {|
  [grantee: $PropertyType<Grantee, 'id'>]: $PropertyType<Role, 'id'>,
|};

export type EntityShareResponse = {|
  active_shares: Array<ActiveShare>,
  available_grantees: Array<Grantee>,
  available_roles: Array<Role>,
  entity: GRN,
  missing_dependencies: Array<MissingDependency>,
  selected_grantee_roles: SelectedGranteeRoles,
|};

export type EntityShareRequest = {|
  selected_grantee_roles?: SelectedGranteeRoles,
|};
