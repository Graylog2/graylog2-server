// @flow strict

export type GRN = string;

export type Capability = {|
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
  capability: GRN,
|};

export type ShareEntity = {|
  id: GRN,
  owners: Array<Grantee>,
  title: string,
  type: string,
|};
