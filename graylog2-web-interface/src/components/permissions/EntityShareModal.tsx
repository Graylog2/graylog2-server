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
import { useRef, useEffect, useState } from 'react';
import type { FormikProps } from 'formik';
import PropTypes from 'prop-types';
import type { $PropertyType } from 'utility-types';

import EntityShareDomain from 'domainActions/permissions/EntityShareDomain';
import { createGRN } from 'logic/permissions/GRN';
import { useStore } from 'stores/connect';
import { Spinner } from 'components/common';
import { EntityShareStore } from 'stores/permissions/EntityShareStore';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';
import type SharedEntity from 'logic/permissions/SharedEntity';
import BootstrapModalConfirm from 'components/bootstrap/BootstrapModalConfirm';

import type { FormValues as GranteesSelectFormValues } from './GranteesSelector';
import EntityShareSettings from './EntityShareSettings';

type Props = {
  description: string,
  entityId: $PropertyType<SharedEntity, 'id'>,
  entityTitle: $PropertyType<SharedEntity, 'title'>,
  entityType: $PropertyType<SharedEntity, 'type'>,
  entityTypeTitle: string | null | undefined,
  onClose: () => void,
  showShareableEntityURL?: boolean
};

const EntityShareModal = ({ description, entityId, entityType, entityTitle, entityTypeTitle, onClose, showShareableEntityURL }: Props) => {
  const { state: entityShareState } = useStore(EntityShareStore);
  const [disableSubmit, setDisableSubmit] = useState(entityShareState?.validationResults?.failed);
  const entityGRN = createGRN(entityType, entityId);
  const granteesSelectFormRef = useRef<FormikProps<GranteesSelectFormValues>>();

  useEffect(() => {
    EntityShareDomain.prepare(entityType, entityTitle, entityGRN);
  }, [entityType, entityTitle, entityGRN]);

  const _handleSave = () => {
    setDisableSubmit(true);
    const selectedGranteeId = granteesSelectFormRef.current?.values?.granteeId;
    const payload: EntitySharePayload = {
      selected_grantee_capabilities: entityShareState.selectedGranteeCapabilities,
    };

    if (selectedGranteeId) {
      const selectedGrantee = entityShareState?.availableGrantees.find((grantee) => grantee.id === selectedGranteeId);

      // eslint-disable-next-line no-alert
      if (!window.confirm(`${selectedGrantee.title ? `"${selectedGrantee.title}"` : 'An entity (name not found)'} got selected but was never added as a collaborator. Do you want to continue anyway?`)) {
        setDisableSubmit(false);

        return;
      }
    }

    EntityShareDomain.update(entityType, entityTitle, entityGRN, payload).then(() => {
      setDisableSubmit(true);
      onClose();
    });
  };

  return (
    <BootstrapModalConfirm confirmButtonDisabled={disableSubmit}
                           confirmButtonText="Update sharing"
                           onConfirm={_handleSave}
                           onCancel={onClose}
                           showModal
                           title={<>Sharing {entityTypeTitle ?? entityType}: <i>{entityTitle}</i></>}>
      {(entityShareState && entityShareState.entity === entityGRN) ? (
        <EntityShareSettings description={description}
                             entityGRN={entityGRN}
                             entityType={entityType}
                             entityTypeTitle={entityTypeTitle}
                             entityTitle={entityTitle}
                             entityShareState={entityShareState}
                             granteesSelectFormRef={granteesSelectFormRef}
                             setDisableSubmit={setDisableSubmit}
                             showShareableEntityURL={showShareableEntityURL} />
      ) : (
        <Spinner />
      )}
    </BootstrapModalConfirm>
  );
};

EntityShareModal.propTypes = {
  description: PropTypes.string.isRequired,
  entityId: PropTypes.string.isRequired,
  entityTitle: PropTypes.string.isRequired,
  entityType: PropTypes.string.isRequired,
  entityTypeTitle: PropTypes.string,
  onClose: PropTypes.func.isRequired,
  showShareableEntityURL: PropTypes.bool,
};

EntityShareModal.defaultProps = {
  entityTypeTitle: undefined,
  showShareableEntityURL: true,
};

export default EntityShareModal;
