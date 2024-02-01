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
import { Formik, Form } from 'formik';

import type { IndexSet } from 'stores/indices/IndexSetsStore';
import type FetchError from 'logic/errors/FetchError';
import IndexSetSelect from 'components/streams/IndexSetSelect';
import UserNotification from 'util/UserNotification';
import { Streams } from '@graylog/server-api';
import { Modal } from 'components/bootstrap';
import ModalSubmit from 'components/common/ModalSubmit';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import StringUtils from 'util/StringUtils';

type ModalProps = {
  descriptor: string,
  indexSets: Array<IndexSet>,
  refetchStreams: () => void,
  toggleShowModal: () => void,
}

type AssignIndexSetFormValues = {
  index_set_id: string | undefined,
}

const AssignIndexSetModal = ({
  toggleShowModal,
  indexSets,
  refetchStreams,
  descriptor,
}: ModalProps) => {
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const modalTitle = `Assign Index Set To ${selectedEntities.length} ${StringUtils.capitalizeFirstLetter(descriptor)}`;
  const onSubmit = ({ index_set_id: indexSetId }: AssignIndexSetFormValues) => Streams.assignToIndexSet(indexSetId, selectedEntities).then(() => {
    refetchStreams();
    UserNotification.success(`Index set was assigned to ${selectedEntities.length} ${descriptor} successfully.`, 'Success');
    setSelectedEntities([]);
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
           show>
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

export default AssignIndexSetModal;
