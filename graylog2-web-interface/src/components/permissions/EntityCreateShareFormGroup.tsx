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

import type SharedEntity from 'logic/permissions/SharedEntity';
import useEntityShareState, { useSetEntityShareState } from 'hooks/useEntityShareState';
import EntityShareDomain from 'domainActions/permissions/EntityShareDomain';
import type { GRN } from 'logic/permissions/types';
import type { GranteesList as GranteesListType, SelectedGranteeCapabilities } from 'logic/permissions/EntityShareState';
import type Grantee from 'logic/permissions/Grantee';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';
import { createGRN } from 'logic/permissions/GRN';
import { Section, Spinner } from 'components/common';
import usePluggableEntityShareFormGroup from 'hooks/usePluggableEntityShareFormGroup';
import type { SelectionRequest } from 'components/permissions/Grantee/GranteesSelector';
import GranteesList from 'components/permissions/Grantee/GranteesList';

import EntityCreateCapabilitySelect from './EntityCreateCapabilitySelect';
import {
  GranteesSelect,
  GranteesSelectOption,
  GranteesSelectorHeadline,
  ShareFormElements,
  ShareFormSection,
  ShareSubmitButton,
  StyledGranteeIcon,
} from './CommonStyledComponents';
import EntityShareValidationsDependencies from './EntityShareValidationsDependencies';

type Props = {
  description: string;
  entityType: SharedEntity['type'];
  entityTitle?: SharedEntity['title'];
  entityId?: string;
  entityTypeTitle?: string | null | undefined;
  defaultSharePayload?: EntitySharePayload;
  onSetEntityShare: (payload: EntitySharePayload) => void;
  dependenciesGRN?: Array<GRN>;
};

const _renderGranteesSelectOption = ({ label, granteeType }: { label: string; granteeType: Grantee['type'] }) => (
  <GranteesSelectOption>
    <StyledGranteeIcon type={granteeType} />
    {label}
  </GranteesSelectOption>
);
const _granteesOptions = (grantees: GranteesListType) =>
  grantees.map((grantee) => ({ label: grantee.title, value: grantee.id, granteeType: grantee.type })).toJS();
const getAvailableGrantee = (grantees: GranteesListType, selected: SelectedGranteeCapabilities) =>
  grantees?.filter((g) => !selected.has(g.id))?.toList();

const EntityCreateShareFormGroup = ({
  description,
  entityType,
  entityTitle = '',
  onSetEntityShare,
  entityId = null,
  entityTypeTitle = '',
  dependenciesGRN = null,
  defaultSharePayload = undefined,
}: Props) => {
  const entityGRN = entityId && createGRN(entityType, entityId);
  const { data: entityShareState } = useEntityShareState(entityGRN);
  const setEntityShareState = useSetEntityShareState();
  const defaultShareSelection = { granteeId: null, capabilityId: 'view' };
  const [disableSubmit, setDisableSubmit] = useState(entityShareState?.validationResults?.failed);
  const [shareSelection, setShareSelection] = useState<SelectionRequest>(defaultShareSelection);
  const [entityShare, setEntityShare] = useState<Omit<EntitySharePayload, 'prepare_request'>>(
    defaultSharePayload ?? null,
  );
  const PluggableEntityShareFormGroup = usePluggableEntityShareFormGroup();

  useEffect(() => {
    const { selected_collections: _, ...rest } = defaultSharePayload ?? {};
    // Restoring a selection has to re-run the dependency check, or the missing dependency
    // warnings shown before would silently disappear.
    const hasSelection = (rest.selected_grantee_capabilities?.size ?? 0) > 0;
    const prepare_request = hasSelection && dependenciesGRN?.length ? dependenciesGRN : null;

    EntityShareDomain.prepare(entityType, entityTitle, entityGRN, { ...rest, prepare_request }).then((state) => {
      setEntityShareState(entityGRN, state);
    });
  }, [entityType, entityTitle, entityGRN, defaultSharePayload, dependenciesGRN, setEntityShareState]);

  const resetSelection = () => {
    setDisableSubmit(false);
    setShareSelection(defaultShareSelection);
  };

  const handleSelection = ({ granteeId, capabilityId }: SelectionRequest) => {
    const newSelectedCapabilities = entityShareState?.selectedGranteeCapabilities.merge({ [granteeId]: capabilityId });

    setDisableSubmit(true);

    const payload: EntitySharePayload = {
      selected_grantee_capabilities: newSelectedCapabilities,
      prepare_request: dependenciesGRN,
    };

    setEntityShare({ ...entityShare, selected_grantee_capabilities: newSelectedCapabilities });

    return EntityShareDomain.prepare(entityType, entityTitle, entityGRN, payload).then((response) => {
      setEntityShareState(entityGRN, response);
      onSetEntityShare({ ...entityShare, selected_grantee_capabilities: newSelectedCapabilities });
      resetSelection();

      return response;
    });
  };

  const handleDeletion = (granteeId: GRN) => {
    const newSelectedCapabilities = entityShareState?.selectedGranteeCapabilities.remove(granteeId);

    setDisableSubmit(true);

    const prepare_request = (newSelectedCapabilities?.size ?? 0) > 0 ? dependenciesGRN : null;
    const payload: EntitySharePayload = {
      selected_grantee_capabilities: newSelectedCapabilities,
      prepare_request,
    };
    setEntityShare({ ...entityShare, selected_grantee_capabilities: newSelectedCapabilities });

    return EntityShareDomain.prepare(entityType, entityTitle, null, payload).then((response) => {
      setEntityShareState(null, response);
      onSetEntityShare({ ...entityShare, selected_grantee_capabilities: newSelectedCapabilities });
      setDisableSubmit(false);

      return response;
    });
  };

  const handleAdditionalFormChange = (values: Partial<EntitySharePayload>) => {
    const newEntityShare = { ...entityShare, ...values };

    setEntityShare(newEntityShare);
    onSetEntityShare(newEntityShare);
  };

  const handleAddCollaborator = () => {
    handleSelection(shareSelection);
  };

  return (
    <Section title="">
      {entityShareState ? (
        <>
          <ShareFormSection>
            <GranteesSelectorHeadline>Add Collaborator</GranteesSelectorHeadline>
            <p>{description}</p>
            <ShareFormElements>
              <GranteesSelect
                onChange={(granteeId) => setShareSelection({ ...shareSelection, granteeId })}
                optionRenderer={_renderGranteesSelectOption}
                options={_granteesOptions(
                  getAvailableGrantee(entityShareState.availableGrantees, entityShareState.selectedGranteeCapabilities),
                )}
                placeholder="Search for users and teams"
                value={shareSelection.granteeId}
              />
              <EntityCreateCapabilitySelect
                onChange={(capabilityId) => setShareSelection({ ...shareSelection, capabilityId })}
                capabilities={entityShareState?.availableCapabilities}
                value={shareSelection.capabilityId}
              />
              <ShareSubmitButton
                bsStyle="primary"
                title="Add Collaborator"
                onClick={handleAddCollaborator}
                disabled={disableSubmit || !shareSelection.granteeId}>
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
          {PluggableEntityShareFormGroup && (
            /* Resolved from the plugin store at render time and cannot be hoisted. */
            /* eslint-disable-next-line react-hooks/static-components */
            <PluggableEntityShareFormGroup
              entityType={entityType}
              onChange={handleAdditionalFormChange}
              value={defaultSharePayload?.selected_collections || []}
            />
          )}
        </>
      ) : (
        <Spinner />
      )}
    </Section>
  );
};

export default EntityCreateShareFormGroup;
