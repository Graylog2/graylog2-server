// @flow strict
import Grantee from './Grantee';

export interface GranteeInterface {
  get title(): $PropertyType<Grantee, 'title'>,
  get id(): $PropertyType<Grantee, 'id'>,
  get type(): $PropertyType<Grantee, 'type'>,
}
