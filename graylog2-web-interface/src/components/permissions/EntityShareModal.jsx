// @flow strict
import * as React from 'react';
import { useEffect } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import type { GRN } from 'logic/permissions/types';
import { useStore } from 'stores/connect';
import { Spinner } from 'components/common';
import { EntityShareStore, EntityShareActions } from 'stores/permissions/EntityShareStore';
import BootstrapModalConfirm from 'components/bootstrap/BootstrapModalConfirm';

import GranteesSelector, { type SelectionRequest } from './GranteesSelector';
import GranteesList from './GranteesList';
import ShareableEnityURL from './ShareableEnityURL';

const StyledGranteesList = styled(GranteesList)`
  margin-top: 20px;
  margin-bottom: 40px;
`;

const _generateGRN = (id, type) => `grn::::${type}:${id}`;

const _filterAvailableGrantees = ({ availableGrantees, selectedGranteeRoles }) => {
  const availableGranteeRolesUserIds = selectedGranteeRoles.entrySeq().map(([granteeGRN]) => granteeGRN);

  return availableGrantees.filter((grantee) => !availableGranteeRolesUserIds.includes(grantee.id));
};

type Props = {
  description: string,
  entityId: string,
  entityType: string,
  title: string,
  onClose: () => void,
};

const EntityShareModal = ({ description, title, entityId, entityType, onClose }: Props) => {
  const { state: entityShareState } = useStore(EntityShareStore);
  const entityGRN = _generateGRN(entityId, entityType);
  const filteredGrantees = entityShareState && _filterAvailableGrantees(entityShareState);

  useEffect(() => {
    EntityShareActions.prepare(entityGRN);
  }, []);

  const handleSave = () => {
    return EntityShareActions.update(entityGRN, {
      grantee_roles: entityShareState.selectedGranteeRoles,
    }).then(onClose);
  };

  const _handleSelection = ({ granteeId, roleId }: SelectionRequest) => {
    return EntityShareActions.prepare(entityGRN, {
      selected_grantee_roles: entityShareState.selectedGranteeRoles.merge({ [granteeId]: roleId }),
    });
  };

  const _handleDeletion = (granteeId: GRN) => {
    return EntityShareActions.prepare(entityGRN, {
      selected_grantee_roles: entityShareState.selectedGranteeRoles.remove(granteeId),
    });
  };

  return (
    <BootstrapModalConfirm onCancel={onClose}
                           onConfirm={handleSave}
                           onModalClose={onClose}
                           title={title}
                           confirmButtonText="Save"
                           showModal>
      <>
        {!entityShareState && <Spinner />}
        {entityShareState && (
          <>
            <p>
              {description}
            </p>
            <GranteesSelector availableGrantees={filteredGrantees}
                              availableRoles={entityShareState.availableRoles}
                              onSubmit={_handleSelection} />
            <StyledGranteesList availableRoles={entityShareState.availableRoles}
                                entityGRN={entityGRN}
                                onRoleChange={_handleSelection}
                                selectedGrantees={entityShareState.selectedGrantees}
                                onDelete={_handleDeletion} />
            <ShareableEnityURL />
          </>
        )}
      </>
    </BootstrapModalConfirm>
  );
};

EntityShareModal.propTypes = {
  description: PropTypes.string.isRequired,
  entityId: PropTypes.string.isRequired,
  entityType: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
};

export default EntityShareModal;
