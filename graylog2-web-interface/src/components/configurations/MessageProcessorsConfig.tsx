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
import { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';
import { Form, Formik } from 'formik';

import { getValueFromInput } from 'util/FormsUtils';
import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { getConfig } from 'components/configurations/helpers';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { Button, Alert, Table, Modal } from 'components/bootstrap';
import { FormikInput, IfPermitted, ModalSubmit, SortableList, Spinner } from 'components/common';
import * as ISODurationUtils from 'util/ISODurationUtils';
import type { FormConfig, Processor, ProcessorConfig } from 'components/configurations/message-processors/Types';
import GlobalProcessingView from 'components/configurations/message-processors/GlobalProcessingView';

const LabelSpan = styled.span(({ theme }) => css`
  margin-left: ${theme.spacings.sm};
  font-weight: bold;
`);


const MessageProcessorsConfig = () => {
  const [showConfigModal, setShowConfigModal] = useState<boolean>(false);
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);
  const [viewConfig, setViewConfig] = useState<FormConfig | undefined>(undefined);
  const [formConfig, setFormConfig] = useState<FormConfig | undefined>(undefined);
  const futureTimestampNormalizationHelpText = "Enable normalisation of timestamps that specify a time significantly ahead of Graylog's own system time. This typically happens when a log source runs on a server with an incorrect system clock. Future timestamps will be normalised to match the time it was first received by either a Graylog Forwarder, or Graylog. It is important to prevent future timestamps when making use of the Warm Tier, as this can otherwise degrade performance.";

  useEffect(() => {
    Promise.all([
      ConfigurationsActions.listMessageProcessorsConfig(ConfigurationType.MESSAGE_PROCESSORS_CONFIG),
      ConfigurationsActions.list(ConfigurationType.GLOBAL_PROCESSING_RULE_CONFIG)
    ]).then(() => {
      const processorConfig = getConfig(ConfigurationType.MESSAGE_PROCESSORS_CONFIG, configuration);
      const globalConfig = getConfig(ConfigurationType.GLOBAL_PROCESSING_RULE_CONFIG, configuration);
      const config = { ...processorConfig, ...globalConfig, enableFutureTimestampNormalization: globalConfig?.grace_period }
      setViewConfig(config)
      setFormConfig(config);
    });

  }, [configuration]);

  const openModal = () => {
    setShowConfigModal(true);
  };

  const closeModal = () => {
    setShowConfigModal(false);
    setFormConfig(viewConfig);
  };

  const hasNoActiveProcessor = () => formConfig.disabled_processors.length >= formConfig.processor_order.length;

  const saveConfig = (values: FormConfig) => {
    if (!hasNoActiveProcessor()) {
      const { processor_order, disabled_processors, grace_period } = values;
      Promise.allSettled([
        ConfigurationsActions.updateMessageProcessorsConfig(ConfigurationType.MESSAGE_PROCESSORS_CONFIG, { processor_order, disabled_processors }),
        ConfigurationsActions.update(ConfigurationType.GLOBAL_PROCESSING_RULE_CONFIG, { grace_period }),
      ]).then(() => {
        closeModal();
      });
    }
  };

  const updateSorting = (newSorting: Array<{id: string, title: string}>, setFieldValue: (key: string, value: Array<Processor>) => void) => {
    const processorOrder = newSorting.map((item) => ({ class_name: item.id, name: item.title }));

    setFieldValue('processor_order', processorOrder);
  };



  const isProcessorEnabled = (processor: Processor, config: ProcessorConfig) => (
    config.disabled_processors.filter((p) => p === processor.class_name).length < 1
  );

  const summary = () => viewConfig?.processor_order?.map((processor, idx) => {
    const status = isProcessorEnabled(processor, viewConfig) ? 'active' : 'disabled';

    return (
      <tr key={processor.name}>
        <td>{idx + 1}</td>
        <td>{processor.name}</td>
        <td>{status}</td>
      </tr>
    );
  });

  const sortableItems = (formValues: FormConfig) => formValues.processor_order.map((processor) => ({ id: processor.class_name, title: processor.name }));


  const handleEnableFutureTimestampNormalisation = (enabled: boolean,  setFieldValue: (key: string, value: string) => void) => {
    if (enabled) {
      setFieldValue('grace_period', formConfig?.grace_period || 'P2D');
    } else {
      setFieldValue('grace_period', undefined);
    }
  };
  const gracePeriodValidator = (milliseconds: number) =>  milliseconds >= 60 * 1000;
  const validateGracePeriodField = (enabled: boolean) => (value: string) => {
    let errorMessage = '';

    if (enabled && !ISODurationUtils.isValidDuration(value, gracePeriodValidator)) {
       errorMessage = 'Grace Period is invalid';
    }

    return errorMessage;
  };

  if (!viewConfig) { return <Spinner />; }

  return (
    <div>
      <GlobalProcessingView gracePeriod={viewConfig.grace_period} />
      <h2>Message Processors Configuration</h2>
      <p>The following message processors are executed in order. Disabled processors will be skipped.</p>

      <Table striped bordered condensed className="top-margin">
        <thead>
          <tr>
            <th>#</th>
            <th>Processor</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {summary()}
        </tbody>
      </Table>

      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>Edit configuration</Button>
      </IfPermitted>
      {showConfigModal && (<Modal show
                                  onHide={closeModal}>
                             <Formik onSubmit={saveConfig} initialValues={formConfig}>
                               {({ isSubmitting, values, setFieldValue, isValid }) => (
                                 <Form>
                                   <Modal.Header closeButton>
                                     <Modal.Title id="dialog_label">Update Message Processors Configuration</Modal.Title>
                                   </Modal.Header>

                                   <Modal.Body>
                                     <>
                                       <h2>Global Processing Rules Configuration</h2>

                                       <FormikInput type="checkbox"
                                         name="enableFutureTimestampNormalization"
                                         id="enableFutureTimestampNormalization"
                                         help={futureTimestampNormalizationHelpText}
                                         onChange={(event) => handleEnableFutureTimestampNormalisation(getValueFromInput(event.target), setFieldValue)}
                                         label={(
                                           <LabelSpan>Future Timestamp Normalization</LabelSpan>
                                         )} />
                                       <FormikInput type="text"
                                         name="grace_period"
                                         id="grace_perod"
                                         placeholder="P1D"
                                         label="Grace Period"
                                         disabled={!values?.enableFutureTimestampNormalization}
                                         help="If Future Timestamp Normalisation is enabled, timestamps specifying a time further ahead of Graylog's own system time than the Grace Period interval will be normalised."
                                         addonAfter={values.enableFutureTimestampNormalization && ISODurationUtils.formatDuration(values.grace_period, gracePeriodValidator, 'invalid')}
                                         validate={validateGracePeriodField(values.enableFutureTimestampNormalization)}
                                         required />

                                       <h2>Message Processors Configuration</h2>
                                       <h3>Order</h3>
                                       <p>Use drag and drop to change the execution order of the message processors.</p>
                                       <SortableList items={sortableItems(values)}
                                         onMoveItem={(newSorting) => updateSorting(newSorting, setFieldValue)}
                                         displayOverlayInPortal />

                                       <h3>Status</h3>
                                       <p>Change the checkboxes to change the status of a message processor.</p>
                                       <Table striped bordered condensed className="top-margin">
                                         <thead>
                                           <tr>
                                             <th>Processor</th>
                                             <th>Enabled</th>
                                           </tr>
                                         </thead>
                                         <tbody>
                                           <MessageProcessorsConfig />
                                         </tbody>
                                       </Table>
                                       {hasNoActiveProcessor() && (
                                         <Alert bsStyle="danger" title="Error">
                                          No active message processor!
                                         </Alert>
                                       )}
                                     </>
                                   </Modal.Body>
                                   <Modal.Footer>
                                     <ModalSubmit onCancel={closeModal}
                                       isSubmitting={isSubmitting}
                                       disabledSubmit={!isValid}
                                       isAsyncSubmit
                                       submitLoadingText="Update configuration"
                                       submitButtonText="Update configuration" />
                                   </Modal.Footer>
                                 </Form>
                               )}
                             </Formik>
                           </Modal>
      )}
    </div>
  );
};

export default MessageProcessorsConfig;
