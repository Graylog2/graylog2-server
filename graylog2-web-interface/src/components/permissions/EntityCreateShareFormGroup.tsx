
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
import { useEffect, useState } from 'react';
import styled from 'styled-components';
import type { $PropertyType } from 'utility-types';
import type { EntitySharePayload } from 'src/actions/permissions/EntityShareActions';

import type SharedEntity from 'logic/permissions/SharedEntity';
import { useStore } from 'stores/connect';
import { EntityShareStore } from 'stores/permissions/EntityShareStore';
import EntityShareDomain from 'domainActions/permissions/EntityShareDomain';
import type { GRN } from 'logic/permissions/types';
import type { CapabilitiesList, GranteesList as GranteesListType, SelectedGranteeCapabilities } from 'logic/permissions/EntityShareState';
import type Grantee from 'logic/permissions/Grantee';

import ValidationError from './ValidationError';
import DependenciesWarning from './DependenciesWarning';
import type { SelectionRequest } from './GranteesSelector';
import GranteeIcon from './GranteeIcon';
import GranteesList from './GranteesList';

import { Button } from '../bootstrap';
import { Select, Spinner } from '../common';

type Props = {
  description: string;
  entityType: $PropertyType<SharedEntity, 'type'>;
  entityTitle: $PropertyType<SharedEntity, 'title'>;
  entityTypeTitle?: string | null | undefined;
  onSetEntityShare: (payload: EntitySharePayload) => void;
};

const Section = styled.div`
  margin-bottom: 25px;

  &:last-child {
    margin-bottom: 0;
  }
`;
const GranteesSelectorHeadline = styled.h5`
  margin-bottom: 10px;
`;
const FormElements = styled.div`
  display: flex;
`;
const SubmitButton = styled(Button)`
  margin-left: 15px;
`;
const GranteesSelect = styled(Select)`
  flex: 1;
`;
const GranteesSelectOption = styled.div`
  display: flex;
  align-items: center;
`;
const StyledGranteeIcon = styled(GranteeIcon)`
  margin-right: 5px;
`;

const _renderGranteesSelectOption = ({
  label,
  granteeType,
}: {
  label: string;
  granteeType: $PropertyType<Grantee, 'type'>;
}) => (
  <GranteesSelectOption>
    <StyledGranteeIcon type={granteeType} />
    {label}
  </GranteesSelectOption>
);
const _capabilitiesOptions = (capabilities: CapabilitiesList) =>
  capabilities.map((capability) => ({ label: capability.title, value: capability.id })).toJS();
const _granteesOptions = (grantees: GranteesListType) =>
  grantees.map((grantee) => ({ label: grantee.title, value: grantee.id, granteeType: grantee.type })).toJS();
const getAvailableGrantee = (grantees: GranteesListType, selected:  SelectedGranteeCapabilities) =>
  grantees?.filter((g) => !selected.has(g.id))?.toList();

const EntityCreateShareFormGroup = ({ description, entityType, entityTitle, onSetEntityShare, entityTypeTitle='' }: Props) => {
  const { state: entityShareState } = useStore(EntityShareStore);
  const defaultShareSelection = { granteeId: null, capabilityId: 'view' };
  const [disableSubmit, setDisableSubmit] = useState(entityShareState?.validationResults?.failed);
  const [shareSelection, setShareSelection] = useState<SelectionRequest>(defaultShareSelection);

  useEffect(() => {
    EntityShareDomain.prepare(entityType, entityTitle, null);
  }, [entityType, entityTitle]);

  const resetSelection = () => {
    setDisableSubmit(false);
    setShareSelection(defaultShareSelection);
  };

  const _handleSelection = ({ granteeId, capabilityId }: SelectionRequest) => {
    const newSelectedCapabilities = entityShareState?.selectedGranteeCapabilities.merge({ [granteeId]: capabilityId });

    setDisableSubmit(true);

    const payload: EntitySharePayload = {
      selected_grantee_capabilities: newSelectedCapabilities,
    };

    return EntityShareDomain.prepare(entityType, entityTitle, null, payload).then((response) => {
      onSetEntityShare(payload);
      resetSelection();
      setDisableSubmit(false);

      return response;
    })
  };

  const _handleDeletion = (granteeId: GRN) => {
    const newSelectedGranteeCapabilities = entityShareState?.selectedGranteeCapabilities.remove(granteeId);

    setDisableSubmit(true);

    const payload: EntitySharePayload = {
      selected_grantee_capabilities: newSelectedGranteeCapabilities,
    };

    return EntityShareDomain.prepare(entityType, entityTitle, null, payload).then((response) => {
      setDisableSubmit(false);

      return response;
    });
  };
  
  const handleAddCollaborator = () => {
    _handleSelection(shareSelection)
  };

  return (
    <>
      {entityShareState ? (
        <>
          <Section>
            <GranteesSelectorHeadline>Add Collaborator</GranteesSelectorHeadline>
            <p>{description}</p>
               <FormElements>
                 <GranteesSelect
                   onChange={(granteeId) => setShareSelection({...shareSelection, granteeId})}
                   optionRenderer={_renderGranteesSelectOption}
                  options={_granteesOptions(getAvailableGrantee(entityShareState.availableGrantees,entityShareState.selectedGranteeCapabilities))}
                   placeholder="Search for users and teams"
                   value={shareSelection.granteeId}
                 />
                 <Select
                   clearable={false}
                   onChange={(capabilityId) => setShareSelection({...shareSelection, capabilityId})}
                   options={_capabilitiesOptions(entityShareState?.availableCapabilities)}
                   placeholder="View"
                   value={shareSelection.capabilityId}
                 />
                 <SubmitButton
                   bsStyle="success"
                   title="Add Collaborator"
                   onClick={handleAddCollaborator}
                   disabled={disableSubmit}
                 >
                    Add Collaborator
                 </SubmitButton>
               </FormElements>
          </Section>
          <Section>
            <GranteesList
              activeShares={entityShareState?.activeShares}
              availableCapabilities={entityShareState?.availableCapabilities}
              entityType={entityType}
              entityTypeTitle={entityTypeTitle}
              onDelete={_handleDeletion}
              onCapabilityChange={_handleSelection}
              selectedGrantees={entityShareState?.selectedGrantees}
              title="Collaborators"
            />
          </Section>
          {entityShareState?.validationResults?.failed && (
            <Section>
              <ValidationError validationResult={entityShareState?.validationResults} availableGrantees={entityShareState?.availableGrantees} />
            </Section>
          )}
          {entityShareState?.missingDependencies?.size > 0 && (
            <Section>
              <DependenciesWarning missingDependencies={entityShareState?.missingDependencies} availableGrantees={entityShareState?.availableGrantees} />
            </Section>
          )}
        </>
      ): (
        <Spinner />
      )}
    </>
  )
};

export default EntityCreateShareFormGroup;
