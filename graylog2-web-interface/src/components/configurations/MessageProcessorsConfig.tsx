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
import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { Button, Alert, Table } from 'components/bootstrap';
import { IfPermitted, SortableList, Spinner } from 'components/common';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';

type Processor = {
  name: string,
  class_name: string
}

type Config = {
  disabled_processors: Array<string>,
  processor_order: Array<Processor>,
}

const MessageProcessorsConfig = () => {
  const [showConfigModal, setShowConfigModal] = useState<boolean>(false);
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);
  const [viewConfig, setViewConfig] = useState<Config | undefined>(undefined);
  const [formConfig, setFormConfig] = useState<Config | undefined>(undefined);

  useEffect(() => {
    ConfigurationsActions.listMessageProcessorsConfig(ConfigurationType.MESSAGE_PROCESSORS_CONFIG).then(() => {
      const config = getConfig(ConfigurationType.MESSAGE_PROCESSORS_CONFIG, configuration);
      setViewConfig(config);
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

  const saveConfig = () => {
    if (!hasNoActiveProcessor()) {
      ConfigurationsActions.updateMessageProcessorsConfig(ConfigurationType.MESSAGE_PROCESSORS_CONFIG, formConfig).then(() => {
        closeModal();
      });
    }
  };

  const updateSorting = (newSorting: Array<{id: string, title: string}>) => {
    const processorOrder = newSorting.map((item) => ({ class_name: item.id, name: item.title }));

    setFormConfig({ ...formConfig, processor_order: processorOrder });
  };

  const toggleStatus = (className: string, enabled: boolean) => {
    const disabledProcessors = formConfig.disabled_processors;

    if (enabled) {
      setFormConfig({ ...formConfig, disabled_processors: [...disabledProcessors, className] });
    } else {
      setFormConfig({ ...formConfig, disabled_processors: disabledProcessors.filter((processorName) => processorName !== className) });
    }
  };

  const isProcessorEnabled = (processor: Processor, config: Config) => (
    config.disabled_processors.filter((p) => p === processor.class_name).length < 1
  );

  const summary = () => viewConfig.processor_order.map((processor, idx) => {
    const status = isProcessorEnabled(processor, viewConfig) ? 'active' : 'disabled';

    return (
      <tr key={processor.name}>
        <td>{idx + 1}</td>
        <td>{processor.name}</td>
        <td>{status}</td>
      </tr>
    );
  });

  const sortableItems = () => formConfig.processor_order.map((processor) => ({ id: processor.class_name, title: processor.name }));

  const statusForm = () => {
    const sortedProcessorOrder = [...formConfig.processor_order].sort((a, b) => naturalSort(a.name, b.name));

    return sortedProcessorOrder.map((processor) => {
      const enabled = isProcessorEnabled(processor, formConfig);

      return (
        <tr key={processor.name}>
          <td>{processor.name}</td>
          <td>
            <input type="checkbox"
                   checked={enabled}
                   onChange={() => toggleStatus(processor.class_name, enabled)} />
          </td>
        </tr>
      );
    });
  };

  if (!viewConfig) { return <Spinner />; }

  return (
    <div>
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

      {showConfigModal && formConfig && (
      <BootstrapModalForm show
                          title="Update Message Processors Configuration"
                          onSubmitForm={saveConfig}
                          onCancel={closeModal}
                          submitButtonText="Update configuration">
        <>
          <h3>Order</h3>
          <p>Use drag and drop to change the execution order of the message processors.</p>
          <SortableList items={sortableItems()} onMoveItem={updateSorting} displayOverlayInPortal />

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
              {statusForm()}
            </tbody>
          </Table>
          {hasNoActiveProcessor() && (
          <Alert bsStyle="danger" title="Error">
            No active message processor!
          </Alert>
          )}
        </>
      </BootstrapModalForm>
      )}
    </div>
  );
};

export default MessageProcessorsConfig;
