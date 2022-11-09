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
import { Formik, Form, Field } from 'formik';
import { useCallback, useMemo, useEffect } from 'react';

import Version from 'util/Version';
import type { StreamRule, StreamRuleType } from 'stores/streams/StreamsStore';
import { Icon, TypeAheadFieldInput, FormikInput, Select, ModalSubmit, InputOptionalInfo } from 'components/common';
import HumanReadableStreamRule from 'components/streamrules//HumanReadableStreamRule';
import { Col, Well, Input, Modal, Row } from 'components/bootstrap';
import { DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';
import { useStore } from 'stores/connect';
import { InputsStore, InputsActions } from 'stores/inputs/InputsStore';
import STREAM_RULE_TYPES from 'logic/streams/streamRuleTypes';

type FormValues = Partial<StreamRule>

const shouldDisplayValueInput = (type: number) => type !== STREAM_RULE_TYPES.FIELD_PRESENCE && type !== STREAM_RULE_TYPES.ALWAYS_MATCHES;
const shouldDisplayFieldInput = (type: number) => type !== STREAM_RULE_TYPES.ALWAYS_MATCHES && type !== STREAM_RULE_TYPES.MATCH_INPUT;

const validate = (values: FormValues) => {
  let errors = {};

  if (!values.type) {
    errors = { ...errors, type: 'Type is required' };
  }

  if (shouldDisplayValueInput(values.type) && !values.value) {
    errors = { ...errors, value: 'Value is required' };
  }

  if (shouldDisplayFieldInput(values.type) && !values.field) {
    errors = { ...errors, field: 'Field is required' };
  }

  return errors;
};

type Props = {
  onSubmit: (streamRuleId: string | undefined | null, currentStreamRule: StreamRule) => Promise<void>,
  initialValues?: Partial<StreamRule>,
  streamRuleTypes: Array<StreamRuleType>,
  title: string,
  onClose: () => void,
  submitButtonText: string
  submitLoadingText: string
};

const StreamRuleModal = ({
  streamRuleTypes,
  title,
  onClose,
  submitButtonText,
  submitLoadingText,
  onSubmit,
  initialValues,
}: Props) => {
  const { inputs } = useStore(InputsStore);

  useEffect(() => {
    InputsActions.list();
  }, []);

  const _onSubmit = useCallback((values) => {
    return onSubmit(initialValues?.id, values).then(() => onClose());
  }, [onSubmit, initialValues?.id, onClose]);

  const streamRuleTypesOptions = useMemo(() => streamRuleTypes?.map(({ id, short_desc }) => ({
    value: id,
    label: short_desc,
  })), [streamRuleTypes]);

  const inputOptions = useMemo(() => inputs.map(({ id, title: inputTitle, name }) => ({ label: `${inputTitle} (${name})`, value: id })), [inputs]);

  return (
    <Modal title={title}
           onHide={onClose}
           show>
      <Formik<FormValues> initialValues={initialValues} onSubmit={_onSubmit} validate={validate}>
        {({ values, setFieldTouched, setFieldValue, isSubmitting, isValid }) => (
          <Form>
            <Modal.Header closeButton>
              <Modal.Title>{title}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
              <Row>
                <Col md={8}>
                  {shouldDisplayFieldInput(values.type) && (
                  <Field name="field">
                    {({ field: { name, value, onChange, onBlur }, meta: { error, touched } }) => (
                      <TypeAheadFieldInput id={name}
                                           onBlur={onBlur}
                                           type="text"
                                           label="Field"
                                           name={name}
                                           error={(error && touched) ? error : undefined}
                                           defaultValue={value}
                                           onChange={onChange} />
                    )}
                  </Field>
                  )}

                  <Field name="type">
                    {({ field: { name, value, onChange }, meta: { error, touched } }) => (
                      <Input label="Type"
                             id="type"
                             error={(error && touched) ? error : undefined}>
                        <Select onBlur={() => setFieldTouched(name, true)}
                                onChange={(newValue: number) => {
                                  if (newValue === STREAM_RULE_TYPES.MATCH_INPUT || newValue === STREAM_RULE_TYPES.ALWAYS_MATCHES) {
                                    setFieldValue('value', undefined);
                                    setFieldValue('field', undefined);
                                  }

                                  return onChange({
                                    target: { value: newValue, name },
                                  });
                                }}
                                options={streamRuleTypesOptions}
                                inputId={name}
                                placeholder="Select a type"
                                inputProps={{ 'aria-label': 'Select a type' }}
                                value={value} />
                      </Input>
                    )}
                  </Field>

                  {shouldDisplayValueInput(values.type) && (
                    values.type === STREAM_RULE_TYPES.MATCH_INPUT
                      ? (
                        <Field name="value">
                          {({ field: { name, value, onChange }, meta: { error, touched } }) => (
                            <Input id="value"
                                   label="Input"
                                   error={(error && touched) ? error : undefined}>
                              <Select onBlur={() => setFieldTouched(name, true)}
                                      onChange={(newValue: string) => onChange({ target: { value: newValue, name } })}
                                      options={inputOptions}
                                      inputId={name}
                                      placeholder="Select an input"
                                      inputProps={{ 'aria-label': 'Select an input' }}
                                      value={value} />
                            </Input>
                          )}
                        </Field>
                      )
                      : <FormikInput id="value" label="Value" name="value" />
                  )}

                  <FormikInput id="inverted" label="Inverted" name="inverted" type="checkbox" />
                  <FormikInput id="description"
                               label={<>Description <InputOptionalInfo /></>}
                               name="description"
                               type="textarea" />

                  <p>
                    <strong>Result:</strong>
                    {' '}
                    <HumanReadableStreamRule streamRule={values} streamRuleTypes={streamRuleTypes} inputs={inputs} />
                  </p>
                </Col>
                <Col md={4}>
                  <Well bsSize="small" className="matcher-github">
                    The server will try to convert to strings or numbers based on the matcher type as well as it can.

                    <br /><br />
                    <Icon name="github" type="brand" />&nbsp;
                    <a href={`https://github.com/Graylog2/graylog2-server/tree/${Version.getMajorAndMinorVersion()}/graylog2-server/src/main/java/org/graylog2/streams/matchers`}
                       target="_blank"
                       rel="noopener noreferrer"> Take a look at the matcher code on GitHub
                    </a>
                    <br /><br />
                    Regular expressions use Java syntax. <DocumentationLink page={DocsHelper.PAGES.STREAMS}
                                                                            title="More information"
                                                                            text={(
                                                                              <Icon name="lightbulb"
                                                                                    type="regular" />
                                                                              )} />
                  </Well>
                </Col>
              </Row>
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

StreamRuleModal.defaultProps = {
  initialValues: {
    field: '',
    type: 1,
    value: '',
    inverted: false,
    description: '',
  },
};

export default StreamRuleModal;
