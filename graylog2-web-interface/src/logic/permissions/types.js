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
