
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
import type { $PropertyType } from 'utility-types';

import type SharedEntity from 'logic/permissions/SharedEntity';
import { useStore } from 'stores/connect';
import { EntityShareStore } from 'stores/permissions/EntityShareStore';
import EntityShareDomain from 'domainActions/permissions/EntityShareDomain';
import type { GRN } from 'logic/permissions/types';
import type { GranteesList as GranteesListType, SelectedGranteeCapabilities } from 'logic/permissions/EntityShareState';
import type Grantee from 'logic/permissions/Grantee';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';
import { Spinner } from 'components/common';

import type { SelectionRequest } from './GranteesSelector';
import GranteesList from './GranteesList';
import EntityCreateCapabilitySelect from './EntityCreateCapabilitySelect';
import { GranteesSelect, GranteesSelectOption, GranteesSelectorHeadline, ShareFormElements, ShareFormSection, ShareSubmitButton, StyledGranteeIcon } from './CommonStyledComponents';
import EntityShareValidationsDependencies from './EntityShareValidationsDependencies';

type Props = {
  description: string;
  entityType: $PropertyType<SharedEntity, 'type'>;
  entityTitle: $PropertyType<SharedEntity, 'title'>;
  entityTypeTitle?: string | null | undefined;
  onSetEntityShare: (payload: EntitySharePayload) => void;
};

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

  const handleSelection = ({ granteeId, capabilityId }: SelectionRequest) => {
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

  const handleDeletion = (granteeId: GRN) => {
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
    handleSelection(shareSelection)
  };

  return (
    <>
      {entityShareState ? (
        <>
          <ShareFormSection>
            <GranteesSelectorHeadline>Add Collaborator</GranteesSelectorHeadline>
            <p>{description}</p>
            <ShareFormElements>
              <GranteesSelect
                onChange={(granteeId) => setShareSelection({...shareSelection, granteeId})}
                optionRenderer={_renderGranteesSelectOption}
                options={_granteesOptions(getAvailableGrantee(entityShareState.availableGrantees,entityShareState.selectedGranteeCapabilities))}
                placeholder="Search for users and teams"
                value={shareSelection.granteeId}
              />
              <EntityCreateCapabilitySelect
                onChange={(capabilityId) => setShareSelection({...shareSelection, capabilityId})}
                capabilities={entityShareState?.availableCapabilities}
                value={shareSelection.capabilityId}
              />
              <ShareSubmitButton
                bsStyle="success"
                title="Add Collaborator"
                onClick={handleAddCollaborator}
                disabled={disableSubmit || !shareSelection.granteeId}
              >
                 Add Collaborator
              </ShareSubmitButton>
            </ShareFormElements>
          </ShareFormSection>
          <ShareFormSection>
            <GranteesList
              activeShares={entityShareState?.activeShares}
              availableCapabilities={entityShareState?.availableCapabilities}
              entityType={entityType}
              entityTypeTitle={entityTypeTitle}
              onDelete={handleDeletion}
              onCapabilityChange={handleSelection}
              selectedGrantees={entityShareState?.selectedGrantees}
              title="Collaborators"
              isCreating
            />
          </ShareFormSection>
          <EntityShareValidationsDependencies
            missingDependencies={entityShareState.missingDependencies}
            validationResults={entityShareState.validationResults}
            availableGrantees={entityShareState.availableGrantees}
           />
        </>
      ): (
        <Spinner />
      )}
    </>
  )
};

export default EntityCreateShareFormGroup;
