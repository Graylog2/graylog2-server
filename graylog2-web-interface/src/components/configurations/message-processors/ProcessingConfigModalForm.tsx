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
import styled, { css } from 'styled-components';
import { Form, Formik } from 'formik';
import { useTranslation } from 'react-i18next';

import { getValueFromInput } from 'util/FormsUtils';
import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { Alert, Table, Modal } from 'components/bootstrap';
import { FormikInput, ModalSubmit, SortableList } from 'components/common';
import * as ISODurationUtils from 'util/ISODurationUtils';
import type { FormConfig, Processor } from 'components/configurations/message-processors/Types';
import MessageProcessorStatusFormGroup from 'components/configurations/message-processors/MessageProcessorStatusFormGroup';
import useProductName from 'brand-customization/useProductName';

type Props = {
  closeModal: () => void;
  formConfig: FormConfig;
};

// ISO8601 duration, not translatable
const DEFAULT_GRACE_PERIOD = 'P2D';

const LabelSpan = styled.span(
  ({ theme }) => css`
    margin-left: ${theme.spacings.sm};
    font-weight: bold;
  `,
);
const ProcessingConfigModalForm = ({ closeModal, formConfig }: Props) => {
  const { t } = useTranslation('configurations');
  const productName = useProductName();
  const hasNoActiveProcessor = () => formConfig.disabled_processors.length >= formConfig.processor_order.length;
  const saveConfig = (values: FormConfig) => {
    if (!hasNoActiveProcessor()) {
      const { processor_order, disabled_processors, grace_period } = values;
      Promise.allSettled([
        ConfigurationsActions.updateMessageProcessorsConfig(ConfigurationType.MESSAGE_PROCESSORS_CONFIG, {
          processor_order,
          disabled_processors,
        }),
        ConfigurationsActions.update(ConfigurationType.GLOBAL_PROCESSING_RULE_CONFIG, { grace_period }),
      ]).then(() => {
        closeModal();
      });
    }
  };
  const updateSorting = (
    newSorting: Array<{ id: string; title: string }>,
    setFieldValue: (key: string, value: Array<Processor>) => void,
  ) => {
    const processorOrder = newSorting.map((item) => ({ class_name: item.id, name: item.title }));

    setFieldValue('processor_order', processorOrder);
  };
  const sortableItems = (formValues: FormConfig) =>
    formValues.processor_order.map((processor) => ({ id: processor.class_name, title: processor.name }));
  const handleEnableFutureTimestampNormalisation = (
    enabled: boolean,
    setFieldValue: (key: string, value: string) => void,
  ) => {
    if (enabled) {
      setFieldValue('grace_period', formConfig?.grace_period || DEFAULT_GRACE_PERIOD);
    } else {
      setFieldValue('grace_period', undefined);
    }
  };
  const gracePeriodValidator = (milliseconds: number) => milliseconds >= 60 * 1000;
  const validateGracePeriodField = (enabled: boolean) => (value: string) => {
    let errorMessage = '';

    if (enabled && !ISODurationUtils.isValidDuration(value, gracePeriodValidator)) {
      errorMessage = t('message_processors.grace_period_invalid');
    }

    return errorMessage;
  };

  return (
    <Modal show data-testid="config-modal" onHide={closeModal}>
      <Formik onSubmit={saveConfig} initialValues={formConfig}>
        {({ isSubmitting, values, setFieldValue, isValid }) => (
          <Form>
            <Modal.Header>
              <Modal.Title>{t('message_processors.update_title')}</Modal.Title>
            </Modal.Header>

            <Modal.Body>
              <>
                <h2>{t('message_processors.global_title')}</h2>
                <FormikInput
                  type="checkbox"
                  name="enableFutureTimestampNormalization"
                  id="enableFutureTimestampNormalization"
                  help={t('message_processors.future_timestamp_help', { productName })}
                  onChange={(event) =>
                    handleEnableFutureTimestampNormalisation(Boolean(getValueFromInput(event.target)), setFieldValue)
                  }
                  label={<LabelSpan>{t('message_processors.future_timestamp_normalization')}</LabelSpan>}
                />
                <FormikInput
                  type="text"
                  name="grace_period"
                  id="grace_period"
                  placeholder={DEFAULT_GRACE_PERIOD}
                  label={t('message_processors.grace_period')}
                  disabled={!values?.enableFutureTimestampNormalization}
                  help={t('message_processors.grace_period_help', { productName })}
                  addonAfter={
                    values.enableFutureTimestampNormalization &&
                    ISODurationUtils.formatDuration(values.grace_period, gracePeriodValidator, t('shared.invalid'))
                  }
                  validate={validateGracePeriodField(values.enableFutureTimestampNormalization)}
                  required
                />
                <h2>{t('message_processors.title')}</h2>
                <h3>{t('message_processors.order')}</h3>
                <p>{t('message_processors.order_help')}</p>
                <SortableList
                  items={sortableItems(values)}
                  onMoveItem={(newSorting) => updateSorting(newSorting, setFieldValue)}
                  displayOverlayInPortal
                />
                <h3>{t('message_processors.status')}</h3>
                <p>{t('message_processors.status_help')}</p>
                <Table bordered condensed className="top-margin">
                  <thead>
                    <tr>
                      <th>{t('message_processors.processor')}</th>
                      <th>{t('message_processors.enabled')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <MessageProcessorStatusFormGroup />
                  </tbody>
                </Table>
                {hasNoActiveProcessor() && (
                  <Alert bsStyle="danger" title={t('shared.error')}>
                    {t('message_processors.no_active_processor')}
                  </Alert>
                )}
              </>
            </Modal.Body>
            <Modal.Footer>
              <ModalSubmit
                onCancel={closeModal}
                isSubmitting={isSubmitting}
                disabledSubmit={!isValid}
                isAsyncSubmit
                submitLoadingText={t('shared.update_configuration')}
                submitButtonText={t('shared.update_configuration')}
              />
            </Modal.Footer>
          </Form>
        )}
      </Formik>
    </Modal>
  );
};

export default ProcessingConfigModalForm;
