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
import { useQueryClient } from '@tanstack/react-query';

import { Streams } from '@graylog/server-api';
import { Modal } from 'components/bootstrap';
import StringUtils from 'util/StringUtils';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type FetchError from 'logic/errors/FetchError';
import ApiRoutes from 'routing/ApiRoutes';
import UserNotification from 'util/UserNotification';
import ModalSubmit from 'components/common/ModalSubmit';
import IfPermitted from 'components/common/IfPermitted';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import MenuItem from 'components/bootstrap/MenuItem';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';

import IndexSetSelect from '../IndexSetSelect';

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
}: {
  descriptor: string,
  indexSets: Array<IndexSet>,
  refetchStreams: () => void,
  setSelectedStreamIds: (streamIds: Array<string>) => void
  selectedStreamIds: Array<string>,
  toggleShowModal: () => void,
}) => {
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

type Props = {
  selectedStreamIds: Array<string>,
  setSelectedStreamIds: (streamIds: Array<string>) => void,
  indexSets: Array<IndexSet>
}

const BulkActions = ({ selectedStreamIds, setSelectedStreamIds, indexSets }: Props) => {
  const [showIndexSetModal, setShowIndexSetModal] = useState(false);
  const queryClient = useQueryClient();

  const selectedItemsAmount = selectedStreamIds?.length;
  const descriptor = StringUtils.pluralize(selectedItemsAmount, 'stream', 'streams');

  const refetchStreams = useCallback(() => queryClient.invalidateQueries(['streams', 'overview']), [queryClient]);

  const onDelete = useCallback(() => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Do you really want to remove ${selectedItemsAmount} ${descriptor}?`)) {
      fetch(
        'POST',
        qualifyUrl(ApiRoutes.StreamsApiController.bulk_delete().url),
        { entity_ids: selectedStreamIds },
      ).then(({ failures }) => {
        if (failures?.length) {
          const notDeletedStreamIds = failures.map(({ entity_id }) => entity_id);
          setSelectedStreamIds(notDeletedStreamIds);
          UserNotification.error(`${notDeletedStreamIds.length} out of ${selectedItemsAmount} selected ${descriptor} could not be deleted.`);
        } else {
          setSelectedStreamIds([]);
          UserNotification.success(`${selectedItemsAmount} ${descriptor} ${StringUtils.pluralize(selectedItemsAmount, 'was', 'were')} deleted successfully.`, 'Success');
        }
      }).catch((error) => {
        UserNotification.error(`An error occurred while deleting streams. ${error}`);
      }).finally(() => {
        refetchStreams();
      });
    }
  }, [
    descriptor,
    refetchStreams,
    selectedItemsAmount,
    selectedStreamIds,
    setSelectedStreamIds,
  ]);

  const toggleAssignIndexSetModal = useCallback(() => {
    setShowIndexSetModal((cur) => !cur);
  }, []);

  return (
    <>
      <BulkActionsDropdown selectedEntities={selectedStreamIds} setSelectedEntities={setSelectedStreamIds}>
        <IfPermitted permissions="indexsets:read">
          <MenuItem onSelect={toggleAssignIndexSetModal}>Assign index set</MenuItem>
        </IfPermitted>
        <MenuItem onSelect={onDelete}>Delete</MenuItem>
      </BulkActionsDropdown>
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

export default BulkActions;
