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
import { useEffect, useState, useRef } from 'react';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { Button, Alert, Table } from 'components/bootstrap';
import { IfPermitted, SortableList } from 'components/common';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';

type Config = {
  disabled_processors: Array<any>,
  processor_order: Array<any>,
}

const MessageProcessorsConfig = () => {
  const [showConfigModal, setShowConfigModal] = useState(false);
  const [config, setConfig] = useState<Config | undefined>(undefined);

  const inputsRef = useRef({});

  useEffect(() => {
    ConfigurationsActions.listMessageProcessorsConfig(ConfigurationType.MESSAGE_PROCESSORS_CONFIG)
      .then((configData) => {
        setConfig(configData as Config);
      });
  }, []);

  const openModal = () => {
    setShowConfigModal(true);
  };

  const closeModal = () => {
    setShowConfigModal(false);
  };

  const hasNoActiveProcessor = () => {
    return config.disabled_processors.length >= config.processor_order.length;
  };

  const saveConfig = () => {
    if (!hasNoActiveProcessor()) {
      ConfigurationsActions.updateMessageProcessorsConfig(ConfigurationType.MESSAGE_PROCESSORS_CONFIG, config).then(() => {
        closeModal();
      });
    }
  };

  const updateSorting = (newSorting) => {
    const processorOrder = newSorting.map((item) => {
      return { class_name: item.id, name: item.title };
    });

    setConfig({ ...config, processor_order: processorOrder });
  };

  const toggleStatus = (className) => {
    return () => {
      let newDisabledProcessors = config.disabled_processors;
      const { checked } = inputsRef.current[className];

      if (checked) {
        newDisabledProcessors = config.disabled_processors.filter((p) => p !== className);
      } else if (config.disabled_processors.indexOf(className) === -1) {
        newDisabledProcessors.push(className);
      }

      setConfig({ ...config, disabled_processors: newDisabledProcessors });
    };
  };

  const summary = () => {
    return config.processor_order.map((processor, idx) => {
      const status = config.disabled_processors.filter((p) => p === processor.class_name).length > 0 ? 'disabled' : 'active';

      return (
        // eslint-disable-next-line react/no-array-index-key
        <tr key={idx}>
          <td>{idx + 1}</td>
          <td>{processor.name}</td>
          <td>{status}</td>
        </tr>
      );
    });
  };

  const sortableItems = () => {
    return config.processor_order.map((processor) => {
      return { id: processor.class_name, title: processor.name };
    });
  };

  const statusForm = () => {
    const sortedProcessorOrder = [...config.processor_order].sort((a, b) => naturalSort(a.name, b.name));

    return sortedProcessorOrder.map((processor, idx) => {
      const enabled = config.disabled_processors.filter((p) => p === processor.class_name).length < 1;

      return (
        // eslint-disable-next-line react/no-array-index-key
        <tr key={idx}>
          <td>{processor.name}</td>
          <td>
            <input ref={(elem) => { inputsRef.current = ({ ...inputsRef.current, [processor.class_name]: elem }); }}
                   type="checkbox"
                   checked={enabled}
                   onChange={toggleStatus(processor.class_name)} />
          </td>
        </tr>
      );
    });
  };

  if (!config) { return null; }

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

      {showConfigModal && (
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
          <Alert bsStyle="danger">
            <strong>ERROR:</strong> No active message processor!
          </Alert>
          )}
        </>
      </BootstrapModalForm>
      )}
    </div>
  );
};

export default MessageProcessorsConfig;
