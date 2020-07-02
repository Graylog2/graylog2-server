// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
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
import DependenciesWarning from './DependenciesWarning';
import ShareableEnityURL from './ShareableEnityURL';

type Props = {
  description: string,
  entityId: string,
  entityTitle: string,
  entityType: string,
  onClose: () => void,
};

type ModalContentProps = {
  description: $PropertyType<Props, 'description'>,
  entityGRN: GRN,
  entityShareState: EntityShareState,
  setDisableSubmit: (boolean) => void,
};

const StyledGranteesList = styled(GranteesList)`
  margin-bottom: 25px;
`;

const GranteesSelectorHeadline = styled.h5`
  margin-bottom: 10px;
`;
const GranteesListHeadline = styled.h5`
  margin-top: 25px;
  margin-bottom: 10px;
`;

const _filterAvailableGrantees = (availableGrantees, selectedGranteeRoles) => {
  const availableGranteeRolesUserIds = selectedGranteeRoles.entrySeq().map(([granteeGRN]) => granteeGRN);

  return availableGrantees.filter((grantee) => !availableGranteeRolesUserIds.includes(grantee.id));
};

const ModalContent = ({
  entityShareState: {
    activeShares,
    availableGrantees,
    availableRoles,
    missingDependencies,
    selectedGranteeRoles,
    selectedGrantees,
  },
  description,
  entityGRN,
  setDisableSubmit,
}: ModalContentProps) => {
  const filteredGrantees = _filterAvailableGrantees(availableGrantees, selectedGranteeRoles);

  const _handleSelection = ({ granteeId, roleId }: SelectionRequest) => {
    setDisableSubmit(true);

    return EntityShareActions.prepare(entityGRN, {
      selected_grantee_roles: selectedGranteeRoles.merge({ [granteeId]: roleId }),
    }).then((response) => {
      setDisableSubmit(false);

      return response;
    });
  };

  const _handleDeletion = (granteeId: GRN) => {
    setDisableSubmit(true);

    return EntityShareActions.prepare(entityGRN, {
      selected_grantee_roles: selectedGranteeRoles.remove(granteeId),
    }).then((response) => {
      setDisableSubmit(false);

      return response;
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
      <StyledGranteesList activeShares={activeShares}
                          availableRoles={availableRoles}
                          entityGRN={entityGRN}
                          onDelete={_handleDeletion}
                          onRoleChange={_handleSelection}
                          selectedGrantees={selectedGrantees} />
      {missingDependencies && (
        <DependenciesWarning missingDependencies={missingDependencies}
                             selectedGrantees={selectedGrantees} />
      )}
      <ShareableEnityURL />
    </>
  );
};

const _generateGRN = (id, type) => `grn::::${type}:${id}`;

const EntityShareModal = ({ description, entityId, entityType, entityTitle, onClose }: Props) => {
  const { state: entityShareState } = useStore(EntityShareStore);
  const [disableSubmit, setDisableSubmit] = useState(false);
  const entityGRN = _generateGRN(entityId, entityType);

  useEffect(() => {
    EntityShareActions.prepare(entityGRN);
  }, []);

  const _handleSave = () => {
    setDisableSubmit(true);

    return EntityShareActions.update(entityGRN, {
      grantee_roles: entityShareState.selectedGranteeRoles,
    }).then(onClose);
  };

  return (
    <BootstrapModalConfirm confirmButtonDisabled={disableSubmit}
                           confirmButtonText="Save"
                           onConfirm={_handleSave}
                           onModalClose={onClose}
                           showModal
                           title={<>Sharing: {entityType}: <i>{entityTitle}</i></>}>
      <>
        {entityShareState ? (
          <ModalContent description={description}
                        entityGRN={entityGRN}
                        entityShareState={entityShareState}
                        setDisableSubmit={setDisableSubmit} />
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
  entityTitle: PropTypes.string.isRequired,
  entityType: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
};

export default EntityShareModal;
