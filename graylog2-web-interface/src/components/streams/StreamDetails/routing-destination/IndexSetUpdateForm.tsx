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
import { useTranslation } from 'react-i18next';

import i18n from 'i18n';
import { Button, Modal } from 'components/bootstrap';
import usePermissions from 'hooks/usePermissions';
import type { Stream } from 'logic/streams/types';
import useStreamMutations from 'hooks/useStreamMutations';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { Icon, ModalSubmit } from 'components/common';
import UserNotification from 'util/UserNotification';
import IndexSetSelect from 'components/streams/IndexSetSelect';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

type FormValues = Partial<Pick<Stream, 'index_set_id'>>;
type Props = {
  initialValues: FormValues;
  indexSets: Array<IndexSet>;
  stream: Stream;
};

const prepareInitialValues = (initialValues: FormValues, indexSets: Array<IndexSet>) => ({
  index_set_id: initialValues.index_set_id ?? indexSets?.find((indexSet) => indexSet.default)?.id,
});

const validate = (values: FormValues) => {
  let errors = {};

  if (!values.index_set_id) {
    errors = { index_set_id: i18n.t('stream_modal.index_set_required', { ns: 'streams' }) };
  }

  return errors;
};

const IndexSetUpdateForm = ({ initialValues, indexSets, stream }: Props) => {
  const { t } = useTranslation('streams');
  const { updateStream } = useStreamMutations();
  const { isPermitted } = usePermissions();
  const [showModal, setShowModal] = useState<boolean>(false);
  const _initialValues = useMemo(() => prepareInitialValues(initialValues, indexSets), [indexSets, initialValues]);
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
    // The api fn handles the error toast on rejection, so we only run the success path on resolve.
    updateStream({ streamId: stream.id, data: values })
      .then(() => {
        UserNotification.success(
          t('routing_destination.index_set_update.updated', { title: stream.title }),
          t('routing_destination.index_set_update.success_title'),
        );
        setShowModal(false);
      })
      .catch(() => {});
  };

  return (
    <>
      <Button
        disabled={!isPermitted(`streams:edit:${stream.id}`)}
        bsSize="sm"
        onClick={openModal}
        title={t('routing_destination.index_set_update.tooltip')}>
        <Icon name="add" /> {t('routing_destination.index_set_update.button')}
      </Button>

      {showModal && (
        <Modal show={showModal} onHide={onCloseModal}>
          <Formik<FormValues> initialValues={_initialValues} onSubmit={onSave} validate={validate}>
            {({ isSubmitting, isValidating }) => (
              <Form>
                <Modal.Header>
                  <Modal.Title>{t('routing_destination.index_set_update.modal_title')}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                  <IndexSetSelect indexSets={indexSets} />
                </Modal.Body>
                <Modal.Footer>
                  <ModalSubmit
                    submitButtonText={t('routing_destination.index_set_update.submit')}
                    submitLoadingText={t('routing_destination.index_set_update.saving')}
                    onCancel={onCloseModal}
                    disabledSubmit={isValidating}
                    isSubmitting={isSubmitting}
                  />
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
