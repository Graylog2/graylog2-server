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
import type { SyntheticEvent } from 'react';
import { useState, useMemo } from 'react';
import { Formik, Form } from 'formik';
import { useQueryClient } from '@tanstack/react-query';

import { Button, Modal } from 'components/bootstrap';
import useCurrentUser from 'hooks/useCurrentUser';
import { StreamsStore, type Stream } from 'stores/streams/StreamsStore';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { Icon, ModalSubmit } from 'components/common';
import UserNotification from 'util/UserNotification';
import { isPermitted } from 'util/PermissionsMixin';
import IndexSetSelect from 'components/streams/IndexSetSelect';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

type FormValues = Partial<Pick<Stream, 'index_set_id'>>
type Props = {
  initialValues: FormValues,
  indexSets: Array<IndexSet>,
  stream: Stream,
};

const prepareInitialValues = (initialValues: FormValues, indexSets: Array<IndexSet>) => ({
  index_set_id: initialValues.index_set_id ?? indexSets?.find((indexSet) => indexSet.default)?.id,
});

const validate = (values: FormValues) => {
  let errors = {};

  if (!values.index_set_id) {
    errors = { index_set_id: 'Index set is required' };
  }

  return errors;
};

const IndexSetUpdateForm = ({ initialValues, indexSets, stream }: Props) => {
  const queryClient = useQueryClient();
  const currentUser = useCurrentUser();
  const [showModal, setShowModal] = useState<boolean>(false);
  const _initialValues = useMemo(
    () => prepareInitialValues(initialValues, indexSets),
    [indexSets, initialValues],
  );
  const sendTelemetry = useSendTelemetry();

  const openModal = (event: SyntheticEvent<HTMLButtonElement>) => {
    event.stopPropagation();
    setShowModal(true);
  };

  const onCloseModal = () => {
    setShowModal(false);

    sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS.STREAM_ITEM_DATA_ROUTING_STREAM_INDEXSET_UPDATE_OPENED, {
      app_pathname: 'streams',
    });
  };

  const onSave = (values: FormValues) => {
    StreamsStore.update(stream.id, values, (response) => {
      UserNotification.success(`IndexSet of stream'${stream.title}' was updated successfully.`, 'Success');
      setShowModal(false);
      queryClient.invalidateQueries(['stream', stream.id]);

      return response;
    });
  };

  return (
    <>
      <Button disabled={!isPermitted(currentUser.permissions, 'stream:edit')}
              bsSize="sm"
              onClick={openModal}
              title="Edit index set">
        <Icon name="add" /> Edit Index Set
      </Button>

      {showModal && (
      <Modal show={showModal}
             onHide={onCloseModal}>
        <Formik<FormValues> initialValues={_initialValues}
                            onSubmit={onSave}
                            validate={validate}>
          {({ isSubmitting, isValidating }) => (
            <Form>
              <Modal.Header closeButton>
                <Modal.Title>Edit Stream IndexSet</Modal.Title>
              </Modal.Header>
              <Modal.Body>
                <IndexSetSelect indexSets={indexSets} />
              </Modal.Body>
              <Modal.Footer>
                <ModalSubmit submitButtonText="Update"
                             submitLoadingText="Saving stream"
                             onCancel={onCloseModal}
                             disabledSubmit={isValidating}
                             isSubmitting={isSubmitting} />
              </Modal.Footer>

            </Form>
          )}
        </Formik>
      </Modal>
      )}
    </>
  );
};

export default IndexSetUpdateForm;
