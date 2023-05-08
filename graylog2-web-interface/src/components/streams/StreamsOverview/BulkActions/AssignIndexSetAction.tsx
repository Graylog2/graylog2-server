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
import { useCallback, useState } from 'react';
import { Formik, Form } from 'formik';

import MenuItem from 'components/bootstrap/MenuItem';
import { isPermitted } from 'util/PermissionsMixin';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import StringUtils from 'util/StringUtils';
import type FetchError from 'logic/errors/FetchError';
import IndexSetSelect from 'components/streams/IndexSetSelect';
import UserNotification from 'util/UserNotification';
import { Streams } from '@graylog/server-api';
import { Modal } from 'components/bootstrap';
import ModalSubmit from 'components/common/ModalSubmit';

type ModalProps = {
  descriptor: string,
  indexSets: Array<IndexSet>,
  refetchStreams: () => void,
  setSelectedStreamIds: (streamIds: Array<string>) => void
  selectedStreamIds: Array<string>,
  toggleShowModal: () => void,
}

type AssignIndexSetFormValues = {
  index_set_id: string | undefined,
}

const AssignIndexSetModal = ({
  selectedStreamIds,
  toggleShowModal,
  indexSets,
  refetchStreams,
  setSelectedStreamIds,
  descriptor,
}: ModalProps) => {
  const modalTitle = `Assign Index Set To ${selectedStreamIds.length} ${StringUtils.capitalizeFirstLetter(descriptor)}`;
  const onSubmit = ({ index_set_id: indexSetId }: AssignIndexSetFormValues) => Streams.assignToIndexSet(indexSetId, selectedStreamIds).then(() => {
    refetchStreams();
    UserNotification.success(`Index set was assigned to ${selectedStreamIds.length} ${descriptor} successfully.`, 'Success');
    setSelectedStreamIds([]);
    toggleShowModal();
  }).catch((error: FetchError) => {
    UserNotification.error(`Assigning index set failed with status: ${error}`, 'Error');
  });

  const validate = ({ index_set_id }: AssignIndexSetFormValues) => {
    let errors = {};

    if (!index_set_id) {
      errors = { ...errors, index_set_id: 'Index set is required' };
    }

    return errors;
  };

  return (
    <Modal title={modalTitle}
           onHide={toggleShowModal}
           show
           data-app-section="stream_bulk_actions"
           data-event-element="Assign Index Set">
      <Formik initialValues={{ index_set_id: undefined }}
              onSubmit={onSubmit}
              validate={validate}>
        {({ isSubmitting, isValidating }) => (
          <Form>
            <Modal.Header closeButton>
              <Modal.Title>{modalTitle}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
              <IndexSetSelect indexSets={indexSets}
                              help="Messages that match the selected streams will be written to the configured index set." />
            </Modal.Body>
            <Modal.Footer>
              <ModalSubmit submitButtonText="Assign index set"
                           submitLoadingText="Assigning index set..."
                           onCancel={toggleShowModal}
                           disabledSubmit={isValidating}
                           isSubmitting={isSubmitting} />
            </Modal.Footer>
          </Form>
        )}
      </Formik>
    </Modal>
  );
};

type Props = {
  descriptor: string,
  indexSets: Array<IndexSet>,
  onSelect?: () => void,
  refetchStreams: () => void,
  selectedStreamIds: Array<string>
  setSelectedStreamIds: (streamIds: Array<string>) => void,
}

const AssignIndexSetAction = ({ indexSets, selectedStreamIds, setSelectedStreamIds, descriptor, refetchStreams, onSelect }: Props) => {
  const [showIndexSetModal, setShowIndexSetModal] = useState(false);

  const toggleAssignIndexSetModal = useCallback(() => {
    if (!showIndexSetModal && typeof onSelect === 'function') {
      onSelect();
    }

    setShowIndexSetModal((cur) => !cur);
  }, [onSelect, showIndexSetModal]);

  if (!isPermitted('indexsets:read')) {
    return null;
  }

  return (
    <>
      <MenuItem onSelect={toggleAssignIndexSetModal}>Assign index set</MenuItem>
      {showIndexSetModal && (
        <AssignIndexSetModal selectedStreamIds={selectedStreamIds}
                             setSelectedStreamIds={setSelectedStreamIds}
                             toggleShowModal={toggleAssignIndexSetModal}
                             indexSets={indexSets}
                             descriptor={descriptor}
                             refetchStreams={refetchStreams} />
      )}
    </>
  );
};

AssignIndexSetAction.defaultProps = {
  onSelect: undefined,
};

export default AssignIndexSetAction;
