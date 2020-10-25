// @flow strict
import * as React from 'react';
import { useRef, useEffect, useState } from 'react';
import PropTypes from 'prop-types';

import EntityShareDomain from 'domainActions/permissions/EntityShareDomain';
import { createGRN } from 'logic/permissions/GRN';
import { useStore } from 'stores/connect';
import { Spinner } from 'components/common';
import { EntityShareStore } from 'stores/permissions/EntityShareStore';
import { type EntitySharePayload } from 'actions/permissions/EntityShareActions';
import SharedEntity from 'logic/permissions/SharedEntity';
import BootstrapModalConfirm from 'components/bootstrap/BootstrapModalConfirm';

import EntityShareSettings from './EntityShareSettings';

type Props = {
  description: string,
  entityId: $PropertyType<SharedEntity, 'id'>,
  entityTitle: $PropertyType<SharedEntity, 'title'>,
  entityType: $PropertyType<SharedEntity, 'type'>,
  onClose: () => void,
};

const EntityShareModal = ({ description, entityId, entityType, entityTitle, onClose }: Props) => {
  const { state: entityShareState } = useStore(EntityShareStore);
  const [disableSubmit, setDisableSubmit] = useState(entityShareState?.validationResult?.failed);
  const entityGRN = createGRN(entityType, entityId);
  const granteesSelectRef = useRef();

  useEffect(() => {
    EntityShareDomain.prepare(entityType, entityTitle, entityGRN);
  }, [entityType, entityTitle, entityGRN]);

  const _handleSave = () => {
    setDisableSubmit(true);
    const granteesSelect = granteesSelectRef?.current;
    const granteesSelectValue = granteesSelect?.state?.value;
    const granteesSelectOptions = granteesSelect?.props?.options;
    const payload: EntitySharePayload = {
      selected_grantee_capabilities: entityShareState.selectedGranteeCapabilities,
    };

    if (granteesSelectValue) {
      const selectedOption = granteesSelectOptions?.find((option) => option.value === granteesSelectValue);

      if (!selectedOption) {
        throw Error(`Can't find ${granteesSelectValue} in grantees select options on save`);
      }

      // eslint-disable-next-line no-alert
      if (!window.confirm(`"${selectedOption.label}" got selected but was never added as a collaborator. Do you want to continue anyway?`)) {
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
                           confirmButtonText="Save"
                           cancelButtonText="Discard changes"
                           onConfirm={_handleSave}
                           onModalClose={onClose}
                           showModal
                           title={<>Sharing {entityType}: <i>{entityTitle}</i></>}>
      <>
        {(entityShareState && entityShareState.entity === entityGRN) ? (
          <EntityShareSettings description={description}
                               entityGRN={entityGRN}
                               entityType={entityType}
                               entityTitle={entityTitle}
                               entityShareState={entityShareState}
                               granteesSelectRef={granteesSelectRef}
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
