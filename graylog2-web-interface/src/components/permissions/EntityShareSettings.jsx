// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import type { GRN } from 'logic/permissions/types';
import EntityShareState from 'logic/permissions/EntityShareState';
import { EntityShareActions } from 'stores/permissions/EntityShareStore';

import GranteesSelector, { type SelectionRequest } from './GranteesSelector';
import GranteesList from './GranteesList';
import DependenciesWarning from './DependenciesWarning';
import ShareableEnityURL from './ShareableEnityURL';

type Props = {
  description: $PropertyType<Props, 'description'>,
  entityGRN: GRN,
  entityShareState: EntityShareState,
  setDisableSubmit: (boolean) => void,
};

const Section = styled.div`
  margin-bottom: 25px;

  :last-child {
    margin-bottom: 0;
  }
`;

const GranteesSelectorHeadline = styled.h5`
  margin-bottom: 10px;
`;

const _filterAvailableGrantees = (availableGrantees, selectedGranteeRoles) => {
  const availableGranteeRolesUserIds = selectedGranteeRoles.entrySeq().map(([granteeGRN]) => granteeGRN);

  return availableGrantees.filter((grantee) => !availableGranteeRolesUserIds.includes(grantee.id));
};

const EntityShareSettings = ({
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
}: Props) => {
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
      <Section>
        <GranteesSelectorHeadline>
          Add collaborator
        </GranteesSelectorHeadline>
        <p>
          {description}
        </p>
      </Section>
      <Section>
        <GranteesSelector availableGrantees={filteredGrantees}
                          availableRoles={availableRoles}
                          onSubmit={_handleSelection} />
      </Section>
      <Section>
        <GranteesList activeShares={activeShares}
                      availableRoles={availableRoles}
                      entityGRN={entityGRN}
                      onDelete={_handleDeletion}
                      onRoleChange={_handleSelection}
                      selectedGrantees={selectedGrantees}
                      title="Current collaborators" />
      </Section>
      {missingDependencies && (
        <Section>
          <DependenciesWarning missingDependencies={missingDependencies}
                               selectedGrantees={selectedGrantees} />
        </Section>
      )}
      <Section>
        <ShareableEnityURL />
      </Section>
    </>
  );
};

EntityShareSettings.propTypes = {
  description: PropTypes.string.isRequired,
  entityGRN: PropTypes.string.isRequired,
  entityShareState: PropTypes.object.isRequired,
  setDisableSubmit: PropTypes.func.isRequired,
};

export default EntityShareSettings;
