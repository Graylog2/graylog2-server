// @flow strict
import * as React from 'react';
import { useContext } from 'react';
import PropTypes from 'prop-types';

import CurrentUserContext from 'contexts/CurrentUserContext';
import { createGRN } from 'logic/permissions/GRN';

type Props = {
  children: React.Node | ({ disabled: boolean }) => React.Node,
  id: string,
  type: string,
  hideChildren: boolean,
};

const HasOwnership = ({ children, id, type, hideChildren }: Props) => {
  const currentUser = useContext(CurrentUserContext);
  const entity = createGRN(id, type);
  const ownership = `entity:own:${entity}`;
  const adminPermission = '*';

  if (currentUser) {
    const { grn_permissions: grnPermissions = [], permissions } = currentUser;
    const isAdmin = permissions.includes(adminPermission);

    if (grnPermissions.includes(ownership) || isAdmin) {
      if (!hideChildren && typeof children === 'function') {
        return <>{ children({ disabled: false }) } </>;
      }

      return <>children</>;
    }
  }

  if (!hideChildren && typeof children === 'function') {
    return <>{ children({ disabled: true }) } </>;
  }

  return null;
};

HasOwnership.propTypes = {
  /** Children to render if user has ownership of the entity */
  children: PropTypes.oneOfType([
    PropTypes.node,
    PropTypes.func,
  ]).isRequired,
  /** The id string which shows entity */
  id: PropTypes.string,
  /** The type of the entity e.g dashboard, stream */
  type: PropTypes.string.isRequired,
  /** Will add disabled=true as a prop to the child in stead of not rendering it */
  hideChildren: PropTypes.bool,
};

HasOwnership.defaultProps = {
  hideChildren: false,
  id: undefined,
};

export default HasOwnership;
