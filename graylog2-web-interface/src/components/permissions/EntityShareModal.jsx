// @flow strict
import * as React from 'react';
import { useEffect } from 'react';
import PropTypes from 'prop-types';

import { useStore } from 'stores/connect';
import { Spinner } from 'components/common';
import EntityShareStore, { EntityShareActions } from 'stores/permissions/EntityShareStore';
import BootstrapModalConfirm from 'components/bootstrap/BootstrapModalConfirm';

import GranteesSelector from './GranteesSelector';

const generateGRN = (id, type) => `grn::::${type}:${id}`;

const _addCollborator = ({ granteeId, roleId }, entityGRN) => {
  EntityShareActions.prepare(entityGRN, {
    selected_grantee_roles: {
      [granteeId]: roleId,
    },
  });
};

type Props = {
  entityId: string,
  entityType: string,
  title: string,
};

const EntityShareModal = ({ title, entityId, entityType }: Props) => {
  const { state: entityShareState } = useStore(EntityShareStore);
  const entityGRN = generateGRN(entityId, entityType);

  useEffect(() => {
    EntityShareActions.prepare(entityGRN);
  }, []);

  return (
    <BootstrapModalConfirm onCancel={() => {}}
                           onConfirm={() => {}}
                           title={title}
                           confirmButtonText="Save"
                           showModal>
      <>
        {!entityShareState && <Spinner />}
        {entityShareState && (
          <>
            <GranteesSelector availableGrantees={entityShareState.availableGrantees}
                              availableRoles={entityShareState.availableRoles}
                              onSubmit={(formData) => _addCollborator(formData, entityGRN)} />
            {/* collaborators list */}
            {/* sharable url box */}
          </>
        )}
      </>
    </BootstrapModalConfirm>
  );
};

EntityShareModal.propTypes = {
  entityId: PropTypes.string.isRequired,
  entityType: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
};

export default EntityShareModal;
