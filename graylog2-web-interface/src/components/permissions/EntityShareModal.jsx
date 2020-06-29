// @flow strict
import * as React from 'react';
import { useEffect } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { useStore } from 'stores/connect';
import { Spinner } from 'components/common';
import EntityShareStore, { EntityShareActions } from 'stores/permissions/EntityShareStore';
import BootstrapModalConfirm from 'components/bootstrap/BootstrapModalConfirm';

import GranteesSelector from './GranteesSelector';
import GranteesList from './GranteesList';

const StyledGranteesList = styled(GranteesList)`
  width: calc(100% - 153px);
  margin-top: 20px;
`;

const _generateGRN = (id, type) => `grn::::${type}:${id}`;

const _filterAvailableGrantees = ({ availableGrantees, activeShares, selectedGranteeRoles }) => {
  const activeSharesUserIds = activeShares.map((activeShare) => activeShare.grantee);
  const availableGranteeRolesUserIds = selectedGranteeRoles.entrySeq().map(([granteeGRN]) => granteeGRN);
  const assignedUserIds = [...activeSharesUserIds, ...availableGranteeRolesUserIds];

  return availableGrantees.filter((grantee) => !assignedUserIds.includes(grantee.id));
};

type Props = {
  entityId: string,
  entityType: string,
  title: string,
  onClose: () => void,
};

const EntityShareModal = ({ title, entityId, entityType, onClose }: Props) => {
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
            <GranteesSelector availableGrantees={filteredGrantees}
                              availableRoles={entityShareState.availableRoles}
                              entityGRN={entityGRN} />
            <StyledGranteesList activeShares={entityShareState.activeShares}
                                availableRoles={entityShareState.availableRoles}
                                entityGRN={entityGRN}
                                availableGrantees={entityShareState.availableGrantees}
                                selectedGranteeRoles={entityShareState.selectedGranteeRoles} />
            {/* sharable url box */}
          </>
        )}
      </>
    </BootstrapModalConfirm>
  );
};

EntityShareModal.propTypes = {
  entityId: PropTypes.string.isRequired,
  entityType: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
};

export default EntityShareModal;
