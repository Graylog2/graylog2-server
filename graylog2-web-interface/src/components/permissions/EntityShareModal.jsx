// @flow strict
import * as React from 'react';
import { useEffect } from 'react';
import PropTypes from 'prop-types';

import { useStore } from 'stores/connect';
import EntityShareStore, { EntityShareActions } from 'stores/permissions/EntityShareStore';
import BootstrapModalConfirm from 'components/bootstrap/BootstrapModalConfirm';

const generateGRN = (id, type) => `grn::::${type}:${id}`;

type Props = {
  entityId: string,
  entityType: string,
  title: string,
};

const EntityShareModal = ({ title, entityId, entityType }: Props) => {
  const { state: entityShareState } = useStore(EntityShareStore);

  useEffect(() => {
    EntityShareActions.prepare(generateGRN(entityId, entityType));
  }, []);

  return (
    <BootstrapModalConfirm onCancel={() => {}}
                           onConfirm={() => {}}
                           title={title}
                           key={entityShareState?.entity}
                           confirmButtonText="Save"
                           showModal>
      <>
        Content
        <textarea>
          {entityShareState && JSON.stringify(entityShareState, null, 2)}
        </textarea>
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
