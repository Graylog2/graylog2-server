// @flow strict
import * as React from 'react';
import { useEffect } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import type { GRN } from 'logic/permissions/types';
import { useStore } from 'stores/connect';
import { Spinner } from 'components/common';
import { EntityShareStore, EntityShareActions } from 'stores/permissions/EntityShareStore';
import EntityShareState from 'logic/permissions/EntityShareState';
import BootstrapModalConfirm from 'components/bootstrap/BootstrapModalConfirm';

import GranteesSelector, { type SelectionRequest } from './GranteesSelector';
import GranteesList from './GranteesList';
import ShareableEnityURL from './ShareableEnityURL';

type Props = {
  description: string,
  entityId: string,
  entityType: string,
  onClose: () => void,
  title: string,
};

type ModalContentProps = {
  description: $PropertyType<Props, 'description'>,
  entityGRN: GRN,
  entityShareState: EntityShareState,
};

const StyledGranteesList = styled(GranteesList)`
  margin-bottom: 20px;
`;

const GranteesSelectorHeadline = styled.h5`
  margin-bottom: 10px;
`;
const GranteesListHeadline = styled.h5`
  margin-top: 20px;
  margin-bottom: 10px;
`;

const _filterAvailableGrantees = (availableGrantees, selectedGranteeRoles) => {
  const availableGranteeRolesUserIds = selectedGranteeRoles.entrySeq().map(([granteeGRN]) => granteeGRN);

  return availableGrantees.filter((grantee) => !availableGranteeRolesUserIds.includes(grantee.id));
};

const ModalContent = ({ entityShareState: { availableGrantees, selectedGranteeRoles, availableRoles, selectedGrantees }, description, entityGRN }: ModalContentProps) => {
  const filteredGrantees = _filterAvailableGrantees(availableGrantees, selectedGranteeRoles);

  const _handleSelection = ({ granteeId, roleId }: SelectionRequest) => {
    return EntityShareActions.prepare(entityGRN, {
      selected_grantee_roles: selectedGranteeRoles.merge({ [granteeId]: roleId }),
    });
  };

  const _handleDeletion = (granteeId: GRN) => {
    return EntityShareActions.prepare(entityGRN, {
      selected_grantee_roles: selectedGranteeRoles.remove(granteeId),
    });
  };

  return (
    <>
      <GranteesSelectorHeadline>
        Add collaborator
      </GranteesSelectorHeadline>
      <p>
        {description}
      </p>
      <GranteesSelector availableGrantees={filteredGrantees}
                        availableRoles={availableRoles}
                        onSubmit={_handleSelection} />
      <GranteesListHeadline>
        Current collaborators
      </GranteesListHeadline>
      <StyledGranteesList availableRoles={availableRoles}
                          entityGRN={entityGRN}
                          onDelete={_handleDeletion}
                          onRoleChange={_handleSelection}
                          selectedGrantees={selectedGrantees} />
      <ShareableEnityURL />
    </>
  );
};

const _generateGRN = (id, type) => `grn::::${type}:${id}`;

const EntityShareModal = ({ description, entityId, entityType, title, onClose }: Props) => {
  const { state: entityShareState } = useStore(EntityShareStore);
  const entityGRN = _generateGRN(entityId, entityType);

  useEffect(() => {
    EntityShareActions.prepare(entityGRN);
  }, []);

  const _handleSave = () => {
    return EntityShareActions.update(entityGRN, {
      grantee_roles: entityShareState.selectedGranteeRoles,
    }).then(onClose);
  };

  return (
    <BootstrapModalConfirm confirmButtonText="Save"
                           onCancel={onClose}
                           onConfirm={_handleSave}
                           onModalClose={onClose}
                           showModal
                           title={title}>
      <>
        {entityShareState ? (
          <ModalContent description={description}
                        entityGRN={entityGRN}
                        entityShareState={entityShareState} />
        ) : (
          <Spinner />
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
