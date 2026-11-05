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
import { useTranslation } from 'react-i18next';

import { Streams } from '@graylog/server-api';

import type { IndexSet } from 'stores/indices/IndexSetsStore';
import type FetchError from 'logic/errors/FetchError';
import IndexSetSelect from 'components/streams/IndexSetSelect';
import UserNotification from 'util/UserNotification';
import { Modal } from 'components/bootstrap';
import ModalSubmit from 'components/common/ModalSubmit';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import i18n from 'i18n';

type ModalProps = {
  indexSets: Array<IndexSet>;
  refetchStreams: () => void;
  toggleShowModal: () => void;
};

type AssignIndexSetFormValues = {
  index_set_id: string | undefined;
};

const AssignIndexSetModal = ({ toggleShowModal, indexSets, refetchStreams }: ModalProps) => {
  const { t } = useTranslation('streams');
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const modalTitle = t('streams_overview.bulk.assign_title', { count: selectedEntities.length });
  const onSubmit = ({ index_set_id: indexSetId }: AssignIndexSetFormValues) =>
    Streams.assignToIndexSet(indexSetId, selectedEntities)
      .then(() => {
        refetchStreams();
        UserNotification.success(
          t('streams_overview.bulk.assign_success', { count: selectedEntities.length }),
          t('streams_overview.bulk.success_title'),
        );
        setSelectedEntities([]);
        toggleShowModal();
      })
      .catch((error: FetchError) => {
        UserNotification.error(
          t('streams_overview.bulk.assign_failed', { error }),
          t('streams_overview.bulk.assign_failed_title'),
        );
      });

  const validate = ({ index_set_id }: AssignIndexSetFormValues) => {
    let errors = {};

    if (!index_set_id) {
      errors = { ...errors, index_set_id: i18n.t('streams_overview.bulk.index_set_required', { ns: 'streams' }) };
    }

    return errors;
  };

  return (
    <Modal onHide={toggleShowModal} show>
      <Formik initialValues={{ index_set_id: undefined }} onSubmit={onSubmit} validate={validate}>
        {({ isSubmitting, isValidating }) => (
          <Form>
            <Modal.Header>
              <Modal.Title>{modalTitle}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
              <IndexSetSelect indexSets={indexSets} help={t('streams_overview.bulk.assign_help')} />
            </Modal.Body>
            <Modal.Footer>
              <ModalSubmit
                submitButtonText={t('streams_overview.bulk.assign_submit')}
                submitLoadingText={t('streams_overview.bulk.assign_loading')}
                onCancel={toggleShowModal}
                disabledSubmit={isValidating}
                isSubmitting={isSubmitting}
              />
            </Modal.Footer>
          </Form>
        )}
      </Formik>
    </Modal>
  );
};

export default AssignIndexSetModal;
