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
import { useCallback, useMemo } from 'react';
import { useTranslation } from 'react-i18next';

import i18n from 'i18n';
import type { StreamRule } from 'logic/streams/types';
import {
  Icon,
  TypeAheadFieldInput,
  FormikInput,
  Select,
  ModalSubmit,
  InputOptionalInfo,
  Spinner,
  BrandIcon,
} from 'components/common';
import HumanReadableStreamRule from 'components/streamrules//HumanReadableStreamRule';
import { Col, Well, Input, Modal, Row } from 'components/bootstrap';
import { DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';
import useStreamRulesInputs from 'hooks/useStreamRulesInputs';
import STREAM_RULE_TYPES from 'logic/streams/streamRuleTypes';
import useStreamRuleTypes from 'components/streams/hooks/useStreamRuleTypes';
import useResourceCustomization from 'brand-customization/useResourceCustomization';
import useServerText from 'util/serverText';

const STREAM_RULE_TYPES_NAMESPACES = ['stream_rule_types'];

type FormValues = Partial<Pick<StreamRule, 'type' | 'field' | 'description' | 'value' | 'inverted'>>;

const shouldDisplayValueInput = (type: number) =>
  type !== STREAM_RULE_TYPES.FIELD_PRESENCE && type !== STREAM_RULE_TYPES.ALWAYS_MATCHES;
const shouldDisplayFieldInput = (type: number) =>
  type !== STREAM_RULE_TYPES.ALWAYS_MATCHES && type !== STREAM_RULE_TYPES.MATCH_INPUT;

const validate = (values: FormValues) => {
  let errors = {};

  if (!values.type) {
    errors = { ...errors, type: i18n.t('stream_rules.modal.type_required', { ns: 'streams' }) };
  }

  if (shouldDisplayValueInput(values.type) && !values.value) {
    errors = { ...errors, value: i18n.t('stream_rules.modal.value_required', { ns: 'streams' }) };
  }

  if (shouldDisplayFieldInput(values.type) && !values.field) {
    errors = { ...errors, field: i18n.t('stream_rules.modal.field_required', { ns: 'streams' }) };
  }

  return errors;
};

type Props = {
  onSubmit: (streamRuleId: string | undefined | null, currentStreamRule: FormValues) => Promise<void>;
  initialValues?: Partial<StreamRule>;
  title: string;
  onClose: () => void;
  submitButtonText: string;
  submitLoadingText: string;
};

const StreamRuleModal = ({
  title,
  onClose,
  submitButtonText,
  submitLoadingText,
  onSubmit,
  initialValues = {
    field: '',
    type: 1,
    value: '',
    inverted: false,
    description: '',
  },
}: Props) => {
  const { t } = useTranslation('streams');
  const { enabled, url } = useResourceCustomization('stream_rule_matcher_code');
  const { data: inputs } = useStreamRulesInputs();
  const { data: streamRuleTypes } = useStreamRuleTypes();
  const serverText = useServerText(STREAM_RULE_TYPES_NAMESPACES);

  const _onSubmit = useCallback(
    (values: FormValues) => onSubmit(initialValues?.id, values).then(() => onClose()),
    [onSubmit, initialValues?.id, onClose],
  );

  const streamRuleTypesOptions = useMemo(
    () =>
      streamRuleTypes?.map(({ id, short_desc }) => ({
        value: id,
        label: serverText(short_desc),
      })),

    [streamRuleTypes, serverText],
  );

  const inputOptions = useMemo(
    () => inputs?.map(({ id, title: inputTitle, name }) => ({ label: `${inputTitle} (${name})`, value: id })),
    [inputs],
  );

  return (
    <Modal onHide={onClose} show>
      <Formik<FormValues> initialValues={initialValues} onSubmit={_onSubmit} validate={validate}>
        {({ values, setFieldValue, isSubmitting, isValidating }) => (
          <Form>
            <Modal.Header>
              <Modal.Title>{title}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
              {!streamRuleTypes?.length ? (
                <Spinner />
              ) : (
                <Row>
                  <Col md={8}>
                    {shouldDisplayFieldInput(values.type) && (
                      <Field name="field">
                        {({ field: { name, value, onChange, onBlur }, meta: { error, touched } }) => (
                          <TypeAheadFieldInput
                            id={name}
                            onBlur={onBlur}
                            label={t('stream_rules.modal.field')}
                            name={name}
                            error={error && touched ? error : undefined}
                            defaultValue={value}
                            onChange={onChange}
                          />
                        )}
                      </Field>
                    )}

                    <Field name="type">
                      {({ field: { name, value, onChange, onBlur }, meta: { error, touched } }) => (
                        <Input
                          label={t('stream_rules.modal.type')}
                          id="type"
                          error={error && touched ? error : undefined}>
                          <Select
                            onBlur={onBlur}
                            onChange={(newValue: number) => {
                              if (
                                newValue === STREAM_RULE_TYPES.MATCH_INPUT ||
                                newValue === STREAM_RULE_TYPES.ALWAYS_MATCHES
                              ) {
                                setFieldValue('value', '');
                                setFieldValue('field', '');
                              }

                              return onChange({
                                target: { value: newValue, name },
                              });
                            }}
                            options={streamRuleTypesOptions}
                            inputId={name}
                            placeholder={t('stream_rules.modal.select_type')}
                            value={value}
                          />
                        </Input>
                      )}
                    </Field>

                    {shouldDisplayValueInput(values.type) &&
                      (values.type === STREAM_RULE_TYPES.MATCH_INPUT ? (
                        <Field name="value">
                          {({ field: { name, value, onChange, onBlur }, meta: { error, touched } }) => (
                            <Input
                              id="value"
                              label={t('stream_rules.modal.input')}
                              error={error && touched ? error : undefined}>
                              <Select
                                onBlur={onBlur}
                                onChange={(newValue: string) => {
                                  onChange({ target: { value: newValue, name } });
                                }}
                                options={inputOptions}
                                inputId={name}
                                placeholder={t('stream_rules.modal.select_input')}
                                aria-label={t('stream_rules.modal.select_input')}
                                value={value}
                              />
                            </Input>
                          )}
                        </Field>
                      ) : (
                        <FormikInput id="value" label={t('stream_rules.modal.value')} name="value" />
                      ))}

                    <FormikInput
                      id="inverted"
                      label={t('stream_rules.modal.inverted')}
                      name="inverted"
                      type="checkbox"
                    />
                    <FormikInput
                      id="description"
                      label={
                        <>
                          {t('stream_rules.modal.description')} <InputOptionalInfo />
                        </>
                      }
                      name="description"
                      type="textarea"
                    />

                    <p>
                      <strong>{t('stream_rules.modal.result')}</strong>{' '}
                      <HumanReadableStreamRule streamRule={values} inputs={inputs} />
                    </p>
                  </Col>
                  <Col md={4}>
                    <Well bsSize="small" className="matcher-github">
                      {t('stream_rules.modal.matcher_info')}
                      {enabled && (
                        <>
                          <br />
                          <br />
                          <BrandIcon name="github" />
                          &nbsp;
                          <a href={url} target="_blank" rel="noopener noreferrer">
                            {' '}
                            {t('stream_rules.modal.matcher_code_link')}
                          </a>
                        </>
                      )}
                      <br />
                      <br />
                      {t('stream_rules.modal.regex_info')}{' '}
                      <DocumentationLink
                        page={DocsHelper.PAGES.STREAMS}
                        title={t('stream_rules.modal.more_information')}
                        text={<Icon name="lightbulb_circle" type="regular" />}
                      />
                    </Well>
                  </Col>
                </Row>
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

export default StreamRuleModal;
