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
import { render, screen } from 'wrappedTestingLibrary';

import CollectorConfigurationModal from './CollectorConfigurationModal';

describe('CollectorConfigurationModal', () => {
  const renderModal = (
    show: boolean = false,
    collectorName: string = '',
    sidecarNames : string[] = [],
    assignedConfigs: string[] = [],
  ) => (
    <CollectorConfigurationModal show={show}
                                 selectedCollectorName={collectorName}
                                 selectedSidecarNames={sidecarNames}
                                 initialAssignedConfigs={assignedConfigs}
                                 initialPartiallyAssignedConfigs={[]}
                                 unassignedConfigs={[]}
                                 onCancel={() => {}}
                                 onSave={() => {}}
                                 getRowData={() => ({
                                   configuration: {
                                     id: 'id',
                                     name: 'name',
                                     color: 'black',
                                     template: '',
                                     collector_id: '',
                                     tags: [],
                                   },
                                   collector: {
                                     id: 'id',
                                     name: 'name',
                                     node_operating_system: 'mac',
                                     service_type: '',
                                     validation_parameters: '',
                                     executable_path: '',
                                     execute_parameters: '',
                                     default_template: '',
                                   },
                                   sidecars: [],
                                   autoAssignedTags: [],
                                 })} />
  );

  it('Should only open modal when show is true', () => {
    render(
      renderModal(
        false,
        'collector1',
      ),
    );

    const modalTitle = screen.queryByText(/collector1/i);

    expect(modalTitle).toBe(null);
  });

  it('Should display in the title the collector name and the selected sidecar names', () => {
    render(
      renderModal(
        true,
        'collector1',
        ['sidecar1', 'sidecar2'],
      ),
    );

    const modalTitle = screen.queryByText(/collector1/i);
    const modalSubTitle = screen.queryByText(/sidecar1, sidecar2/i);

    expect(modalTitle).not.toBe(null);
    expect(modalSubTitle).not.toBe(null);
  });

  it('Should display empty list message and a possibility to create a new config', () => {
    render(
      renderModal(
        true,
        'collector1',
        ['sidecar1', 'sidecar2'],
      ),
    );

    const emptyListMasg = screen.queryByText(/No configurations available for the selected log collector./i);
    const addNewConfig = screen.queryByText(/Add a new configuration/i);

    expect(emptyListMasg).not.toBe(null);
    expect(addNewConfig).not.toBe(null);
  });

  it('Should display assigned config names', () => {
    render(
      renderModal(
        true,
        'collector1',
        ['sidecar1', 'sidecar2'],
        ['config1', 'config2', 'config3'],
      ),
    );

    const config1 = screen.queryByText(/config1/i);
    const config2 = screen.queryByText(/config2/i);
    const config3 = screen.queryByText(/config3/i);

    expect(config1).not.toBe(null);
    expect(config2).not.toBe(null);
    expect(config3).not.toBe(null);
  });
});
