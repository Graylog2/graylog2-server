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

import React, { useMemo, useCallback } from 'react';
import { Formik, Form } from 'formik';
import { useTranslation } from 'react-i18next';

import i18n from 'i18n';
import type { Stream } from 'logic/streams/types';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { FormikInput, ModalSubmit, InputOptionalInfo } from 'components/common';
import { Modal } from 'components/bootstrap';
import IndexSetSelect from 'components/streams/IndexSetSelect';
import EntityCreateShareFormGroup from 'components/permissions/EntityCreateShareFormGroup';
import type { EntityShare, EntitySharePayload } from 'actions/permissions/EntityShareActions';

type FormValues = Partial<
  Pick<Stream, 'title' | 'description' | 'index_set_id' | 'remove_matches_from_default_stream'>
> & {
  share_request?: EntitySharePayload;
};

const prepareInitialValues = (initialValues: FormValues, indexSets: Array<IndexSet>) => ({
  index_set_id: initialValues.index_set_id ?? indexSets?.find((indexSet) => indexSet.default)?.id,
  description: initialValues.description ?? undefined,
  title: initialValues.title ?? undefined,
  remove_matches_from_default_stream: initialValues.remove_matches_from_default_stream ?? undefined,
  share_request: initialValues.share_request ?? undefined,
});

const validate = (values: FormValues) => {
  let errors = {};

  if (!values.title) {
    errors = { ...errors, title: i18n.t('stream_modal.title_required', { ns: 'streams' }) };
  }

  if (!values.index_set_id) {
    errors = { ...errors, index_set_id: i18n.t('stream_modal.index_set_required', { ns: 'streams' }) };
  }

  return errors;
};

type Props = {
  initialValues?: FormValues;
  title: string;
  submitButtonText: string;
  submitLoadingText: string;
  onClose: () => void;
  onSubmit: (values: Partial<Stream> & EntityShare) => Promise<void>;
  indexSets: Array<IndexSet>;
  isNew?: boolean;
};

const StreamModal = ({
  initialValues = {
    title: '',
    description: '',
    remove_matches_from_default_stream: true,
    share_request: null,
  },
  title: modalTitle,
  submitButtonText,
  submitLoadingText,
  onClose,
  onSubmit,
  indexSets,
  isNew = false,
}: Props) => {
  const { t } = useTranslation('streams');
  const _initialValues = useMemo(() => prepareInitialValues(initialValues, indexSets), [indexSets, initialValues]);

  const _onSubmit = useCallback((values: FormValues) => onSubmit(values).then(() => onClose()), [onClose, onSubmit]);

  return (
    <Modal onHide={onClose} show>
      <Formik<FormValues> initialValues={_initialValues} onSubmit={_onSubmit} validate={validate}>
        {({ isSubmitting, isValidating, setFieldValue }) => (
          <Form>
            <Modal.Header>
              <Modal.Title>{modalTitle}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
              <FormikInput
                label={t('stream_modal.title')}
                name="title"
                id="title"
                help={t('stream_modal.title_help')}
              />
              <FormikInput
                label={
                  <>
                    {t('stream_modal.description')} <InputOptionalInfo />
                  </>
                }
                name="description"
                id="description"
                help={t('stream_modal.description_help')}
              />
              <IndexSetSelect indexSets={indexSets} />
              <FormikInput
                label={t('stream_modal.remove_matches_label')}
                help={<span>{t('stream_modal.remove_matches_help')}</span>}
                name="remove_matches_from_default_stream"
                id="remove_matches_from_default_stream"
                type="checkbox"
              />
              {isNew && (
                <EntityCreateShareFormGroup
                  description={t('stream_modal.share_description')}
                  entityType="stream"
                  entityTitle=""
                  onSetEntityShare={(payload) => setFieldValue('share_request', payload)}
                />
              )}
            </Modal.Body>
            <Modal.Footer>
              <ModalSubmit
                submitButtonText={submitButtonText}
                submitLoadingText={submitLoadingText}
                onCancel={onClose}
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

export default StreamModal;
