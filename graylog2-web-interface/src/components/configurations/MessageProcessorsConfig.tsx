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

import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { getConfig } from 'components/configurations/helpers';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { Button, Table } from 'components/bootstrap';
import { IfPermitted, Spinner } from 'components/common';
import type { FormConfig, Processor, ProcessorConfig } from 'components/configurations/message-processors/Types';
import GlobalProcessingView from 'components/configurations/message-processors/GlobalProcessingView';

import ProcessingConfigModalForm from './message-processors/ProcessingConfigModalForm';

const MessageProcessorsConfig = () => {
  const [showConfigModal, setShowConfigModal] = useState<boolean>(false);
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);
  const [viewConfig, setViewConfig] = useState<FormConfig | undefined>(undefined);
  const [formConfig, setFormConfig] = useState<FormConfig | undefined>(undefined);

  useEffect(() => {
    Promise.all([
      ConfigurationsActions.listMessageProcessorsConfig(ConfigurationType.MESSAGE_PROCESSORS_CONFIG),
      ConfigurationsActions.list(ConfigurationType.GLOBAL_PROCESSING_RULE_CONFIG)
    ]).then(() => {
      const processorConfig = getConfig(ConfigurationType.MESSAGE_PROCESSORS_CONFIG, configuration);
      const globalConfig = getConfig(ConfigurationType.GLOBAL_PROCESSING_RULE_CONFIG, configuration);
      const config = { ...processorConfig, ...globalConfig, enableFutureTimestampNormalization: !!globalConfig?.grace_period }
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
      {showConfigModal && (
        <ProcessingConfigModalForm closeModal={closeModal}
                                   formConfig={formConfig} />
      )}
    </div>
  );
};

export default MessageProcessorsConfig;
