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
import type { Stream } from 'src/stores/streams/StreamsStore';
import { Formik, Form, Field } from 'formik';

import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { FormikInput, ModalSubmit, Select, InputOptionalInfo } from 'components/common';
import { Modal, Input } from 'components/bootstrap';

type FormValues = Partial<Pick<Stream, 'title' | 'description' | 'index_set_id' | 'remove_matches_from_default_stream'>>

const prepareInitialValues = (initialValues: FormValues, indexSets: Array<IndexSet>) => {
  return {
    index_set_id: initialValues.index_set_id ?? indexSets?.find((indexSet) => indexSet.default)?.id,
    ...initialValues,
  };
};

const validate = (values: FormValues) => {
  let errors = {};

  if (!values.title) {
    errors = { ...errors, title: 'Title is required' };
  }

  if (!values.index_set_id) {
    errors = { ...errors, index_set_id: 'Index set is required' };
  }

  return errors;
};

type Props = {
  initialValues?: FormValues
  title: string,
  submitButtonText: string,
  submitLoadingText: string,
  onClose: () => void,
  onSubmit: (values: FormValues) => Promise<void>
  indexSets: Array<IndexSet>,
}

const StreamModal = ({
  initialValues,
  title: modalTitle,
  submitButtonText,
  submitLoadingText,
  onClose,
  onSubmit,
  indexSets,
}: Props) => {
  const _initialValues = useMemo(
    () => prepareInitialValues(initialValues, indexSets),
    [indexSets, initialValues],
  );

  const indexSetOptions = useMemo(
    () => indexSets
      .filter((indexSet) => indexSet.can_be_default)
      .map(({ id, title }) => ({
        value: id,
        label: title,
      })),
    [indexSets],
  );

  const _onSubmit = useCallback(
    (values: FormValues) => onSubmit(values).then(() => onClose()),
    [onClose, onSubmit],
  );

  return (
    <Modal title={modalTitle}
           onHide={onClose}
           show>
      <Formik<FormValues> initialValues={_initialValues}
                          onSubmit={_onSubmit}
                          validate={validate}>
        {({ setFieldTouched, isSubmitting, isValid }) => (
          <Form>
            <Modal.Header closeButton>
              <Modal.Title>{modalTitle}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
              <FormikInput label="Title"
                           name="title"
                           id="title"
                           help="A descriptive name of the new stream" />
              <FormikInput label={<>Description <InputOptionalInfo /></>}
                           name="description"
                           id="description"
                           help="What kind of messages are routed into this stream?" />

              <Field name="index_set_id">
                {({ field: { name, value, onChange }, meta: { error, touched } }) => (
                  <Input label="Index Set"
                         help="Messages that match this stream will be written to the configured index set."
                         error={(error && touched) ? error : undefined}>
                    <Select onBlur={() => setFieldTouched(name, true)}
                            onChange={(newValue: number) => onChange({
                              target: { value: newValue, name },
                            })}
                            options={indexSetOptions}
                            inputId={name}
                            placeholder="Select an index set"
                            inputProps={{ 'aria-label': 'Select an index set' }}
                            value={value} />
                  </Input>
                )}
              </Field>

              <FormikInput label={<>Remove matches from &lsquo;Default Stream&rsquo;</>}
                           help={<span>Don&apos;t assign messages that match this stream to the &lsquo;Default Stream&rsquo;.</span>}
                           name="remove_matches_from_default_stream"
                           id="remove_matches_from_default_stream"
                           type="checkbox" />

            </Modal.Body>
            <Modal.Footer>
              <ModalSubmit submitButtonText={submitButtonText}
                           submitLoadingText={submitLoadingText}
                           onCancel={onClose}
                           disabledSubmit={!isValid}
                           isSubmitting={isSubmitting} />
            </Modal.Footer>
          </Form>
        )}
      </Formik>
    </Modal>
  );
};

StreamModal.defaultProps = {
  initialValues: {
    title: '',
    description: '',
    remove_matches_from_default_stream: false,
  },
};

export default StreamModal;
