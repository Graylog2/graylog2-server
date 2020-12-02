/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { useEffect } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { $PropertyType } from 'utility-types';

import type { GRN } from 'logic/permissions/types';
import EntityShareState from 'logic/permissions/EntityShareState';
import SharedEntity from 'logic/permissions/SharedEntity';
import EntityShareDomain from 'domainActions/permissions/EntityShareDomain';
import { EntitySharePayload } from 'actions/permissions/EntityShareActions';
import { Select } from 'components/common';

import GranteesSelector, { SelectionRequest } from './GranteesSelector';
import GranteesList from './GranteesList';
import DependenciesWarning from './DependenciesWarning';
import ValidationError from './ValidationError';
import ShareableEntityURL from './ShareableEntityURL';

type Props = {
  entityGRN: GRN,
  description: string,
  entityType: $PropertyType<SharedEntity, 'type'>,
  entityTitle: $PropertyType<SharedEntity, 'title'>,
  entityShareState: EntityShareState,
  setDisableSubmit: (boolean) => void,
  granteesSelectRef: typeof Select | null | undefined,
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
    validationResults,
  },
  description,
  entityGRN,
  entityType,
  entityTitle,
  setDisableSubmit,
  granteesSelectRef,
}: Props) => {
  const filteredGrantees = _filterAvailableGrantees(availableGrantees, selectedGranteeCapabilities);

  useEffect(() => {
    setDisableSubmit(validationResults?.failed);
  }, [validationResults, setDisableSubmit]);

  const _handleSelection = ({ granteeId, capabilityId }: SelectionRequest) => {
    const newSelectedCapabilities = selectedGranteeCapabilities.merge({ [granteeId]: capabilityId });

    setDisableSubmit(true);

    const payload: EntitySharePayload = {
      selected_grantee_capabilities: newSelectedCapabilities,
    };

    return EntityShareDomain.prepare(entityType, entityTitle, entityGRN, payload);
  };

  const _handleDeletion = (granteeId: GRN) => {
    const newSelectedGranteeCapabilities = selectedGranteeCapabilities.remove(granteeId);

    setDisableSubmit(true);

    const payload: EntitySharePayload = {
      selected_grantee_capabilities: newSelectedGranteeCapabilities,
    };

    return EntityShareDomain.prepare(entityType, entityTitle, entityGRN, payload);
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
        <GranteesSelector availableGrantees={filteredGrantees}
                          availableCapabilities={availableCapabilities}
                          onSubmit={_handleSelection}
                          granteesSelectRef={granteesSelectRef} />
      </Section>
      <Section>
        <GranteesList activeShares={activeShares}
                      availableCapabilities={availableCapabilities}
                      entityType={entityType}
                      onDelete={_handleDeletion}
                      onCapabilityChange={_handleSelection}
                      selectedGrantees={selectedGrantees}
                      title="Current collaborators" />
      </Section>
      {validationResults?.failed && (
        <Section>
          <ValidationError validationResult={validationResults}
                           availableGrantees={availableGrantees} />
        </Section>
      )}
      {missingDependencies?.size > 0 && (
        <Section>
          <DependenciesWarning missingDependencies={missingDependencies}
                               availableGrantees={availableGrantees} />
        </Section>
      )}
      <Section>
        <ShareableEntityURL entityGRN={entityGRN} />
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
