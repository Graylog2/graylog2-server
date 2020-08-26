// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import type { GRN } from 'logic/permissions/types';
import EntityShareState from 'logic/permissions/EntityShareState';
import SharedEntity from 'logic/permissions/SharedEntity';
import EntityShareDomain from 'domainActions/permissions/EntityShareDomain';
import { type EntitySharePayload } from 'actions/permissions/EntityShareActions';
import { Select } from 'components/common';
import SelectedGranteeValidation from 'logic/permissions/SelectedGranteeValidation';

import GranteesSelector, { type SelectionRequest } from './GranteesSelector';
import GranteesList from './GranteesList';
import DependenciesWarning from './DependenciesWarning';
import ValidationError from './ValidationError';
import ShareableEnityURL from './ShareableEnityURL';

type Props = {
  description: $PropertyType<Props, 'description'>,
  entityGRN: GRN,
  entityType: $PropertyType<SharedEntity, 'type'>,
  entityTitle: $PropertyType<SharedEntity, 'title'>,
  entityShareState: EntityShareState,
  setDisableSubmit: (boolean) => void,
  granteesSelectRef: ?Select,
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

const _filterAvailableGrantees = (availableGrantees, selectedGranteeCapabilities) => {
  const availableGranteeCapabilitiesUserIds = selectedGranteeCapabilities.entrySeq().map(([granteeGRN]) => granteeGRN);

  return availableGrantees.filter((grantee) => !availableGranteeCapabilitiesUserIds.includes(grantee.id));
};

const EntityShareSettings = ({
  entityShareState: {
    activeShares,
    availableGrantees,
    availableCapabilities,
    missingDependencies,
    selectedGranteeCapabilities,
    selectedGrantees,
  },
  description,
  entityGRN,
  entityType,
  entityTitle,
  setDisableSubmit,
  granteesSelectRef,
}: Props) => {
  const filteredGrantees = _filterAvailableGrantees(availableGrantees, selectedGranteeCapabilities);
  const [errors, setErrors] = useState(SelectedGranteeValidation(selectedGranteeCapabilities));

  const _handleSelection = ({ granteeId, capabilityId }: SelectionRequest) => {
    const newSelectedCapabilities = selectedGranteeCapabilities.merge({ [granteeId]: capabilityId });

    setErrors(SelectedGranteeValidation(newSelectedCapabilities));
    setDisableSubmit(true);

    const payload: EntitySharePayload = {
      selected_grantee_capabilities: newSelectedCapabilities,
    };

    return EntityShareDomain.prepare(entityType, entityTitle, entityGRN, payload).then((response) => {
      setDisableSubmit(false);

      return response;
    });
  };

  const _handleDeletion = (granteeId: GRN) => {
    const newSelectedGranteeCapabilities = selectedGranteeCapabilities.remove(granteeId);

    setErrors(SelectedGranteeValidation(newSelectedGranteeCapabilities));
    setDisableSubmit(true);

    const payload: EntitySharePayload = {
      selected_grantee_capabilities: newSelectedGranteeCapabilities,
    };

    return EntityShareDomain.prepare(entityType, entityTitle, entityGRN, payload).then((response) => {
      setDisableSubmit(false);

      return response;
    });
  };

  useEffect(() => setDisableSubmit(!errors.valid), [errors]);

  return (
    <>
      <Section>
        <GranteesSelectorHeadline>
          Add collaborator
        </GranteesSelectorHeadline>
        <p>
          {description}
        </p>
        <GranteesSelector availableGrantees={filteredGrantees}
                          availableCapabilities={availableCapabilities}
                          onSubmit={_handleSelection}
                          granteesSelectRef={granteesSelectRef} />
      </Section>
      <Section>
        <GranteesList activeShares={activeShares}
                      availableCapabilities={availableCapabilities}
                      entityGRN={entityGRN}
                      entityType={entityType}
                      onDelete={_handleDeletion}
                      onCapabilityChange={_handleSelection}
                      selectedGrantees={selectedGrantees}
                      title="Current collaborators" />
      </Section>
      {errors && !errors.valid && (
        <Section>
          <ValidationError validationResult={errors} />
        </Section>
      )}
      {missingDependencies && missingDependencies.size > 0 && (
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
