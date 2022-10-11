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
/* eslint-disable */
import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import styled, { css } from 'styled-components';

import { naturalSortIgnoreCase } from 'util/SortUtils';
import { Link } from 'components/common/router';
import { ControlledTableList, PaginatedList, IconButton } from 'components/common';
import Routes from 'routing/Routes';
import { Col, Row, Input } from 'components/bootstrap';
import ColorLabel from 'components/sidecars/common/ColorLabel';
import OperatingSystemIcon from 'components/sidecars/common/OperatingSystemIcon';
import SidecarSearchForm from 'components/sidecars/common/SidecarSearchForm';
import StatusIndicator from 'components/sidecars/common/StatusIndicator';
import commonStyle from 'components/sidecars/common/CommonSidecarStyles.css';

import CollectorsAdministrationActions from './CollectorsAdministrationActions';
import CollectorsAdministrationFilters from './CollectorsAdministrationFilters';
import CollectorConfigurationModalContainer from './CollectorConfigurationModalContainer';
import FiltersSummary from './FiltersSummary';
import style from './CollectorsAdministration.css';

const HeaderComponentsWrapper = styled.div(({ theme }) => css`
  float: right;
  margin: 5px 0;

  .btn-link {
    color: ${theme.colors.variant.darker.default};
  }
`);

const DisabledCollector = styled.div(({ theme }) => css`
  color: ${theme.colors.variant.light.default};
`);

export const PAGE_SIZES = [10, 25, 50, 100];

const CollectorsAdministration = createReactClass({
  // eslint-disable-next-line react/no-unused-class-component-methods
  propTypes: {
    sidecarCollectorPairs: PropTypes.array.isRequired,
    collectors: PropTypes.array.isRequired,
    configurations: PropTypes.array.isRequired,
    pagination: PropTypes.object.isRequired,
    query: PropTypes.string.isRequired,
    filters: PropTypes.object.isRequired,
    onPageChange: PropTypes.func.isRequired,
    onFilter: PropTypes.func.isRequired,
    onQueryChange: PropTypes.func.isRequired,
    onConfigurationChange: PropTypes.func.isRequired,
    onProcessAction: PropTypes.func.isRequired,
  },

  getInitialState() {
    const { sidecarCollectorPairs } = this.props;

    return {
      enabledCollectors: this.getEnabledCollectors(sidecarCollectorPairs),
      selected: [],
      showConfigurationModal: false,
    };
  },

  UNSAFE_componentWillReceiveProps(nextProps) {
    const { sidecarCollectorPairs } = this.props;

    if (!lodash.isEqual(sidecarCollectorPairs, nextProps.sidecarCollectorPairs)) {
      this.setState({
        enabledCollectors: this.getEnabledCollectors(nextProps.sidecarCollectorPairs),
        selected: this.filterSelectedCollectors(nextProps.sidecarCollectorPairs),
      });
    }
  },

  componentDidUpdate() {
    const { enabledCollectors, selected } = this.state;

    this.setSelectAllCheckboxState(this.selectAllInput, enabledCollectors, selected);
  },

  // Filter out sidecars with no compatible collectors
  getEnabledCollectors(collectors) {
    return collectors.filter(({ collector }) => !lodash.isEmpty(collector));
  },

  setSelectAllCheckboxState(selectAllInput, collectors, selected) {
    const selectAllCheckbox = selectAllInput ? selectAllInput.getInputDOMNode() : undefined;

    if (!selectAllCheckbox) {
      return;
    }

    // Set the select all checkbox as indeterminate if some but not all items are selected.
    selectAllCheckbox.indeterminate = selected.length > 0 && !this.isAllSelected(collectors, selected);
  },

  sidecarCollectorId(sidecar, collector) {
    return `${sidecar.node_id}-${collector.name}`;
  },

  filterSelectedCollectors(collectors) {
    const { selected } = this.state;
    const filteredSidecarCollectorIds = collectors.map(({ collector, sidecar }) => this.sidecarCollectorId(sidecar, collector));

    return selected.filter((sidecarCollectorId) => filteredSidecarCollectorIds.includes(sidecarCollectorId));
  },

  // eslint-disable-next-line react/sort-comp
  handleConfigurationChange(selectedSidecars, selectedConfigurations, doneCallback) {
    const { onConfigurationChange } = this.props;
    onConfigurationChange(selectedSidecars, selectedConfigurations, doneCallback);
  },

  handleProcessAction(action, selectedSidecarCollectorPairs, doneCallback) {
    const { onProcessAction } = this.props;
    const selectedCollectors = {};

    selectedSidecarCollectorPairs.forEach(({ sidecar, collector }) => {
      if (selectedCollectors[sidecar.node_id]) {
        selectedCollectors[sidecar.node_id].push(collector.id);
      } else {
        selectedCollectors[sidecar.node_id] = [collector.id];
      }
    });

    onProcessAction(action, selectedCollectors, doneCallback);
  },

  // eslint-disable-next-line react/no-unstable-nested-components
  formatHeader(selectedSidecarCollectorPairs) {
    const { collectors, configurations } = this.props;
    const { selected, enabledCollectors } = this.state;
    const selectedItems = selected.length;

    let headerMenu;

    if (selectedItems === 0) {
      const { filters, onFilter } = this.props;

      headerMenu = (
        <CollectorsAdministrationFilters collectors={collectors}
                                         configurations={configurations}
                                         filters={filters}
                                         filter={onFilter} />
      );
    } else {
      headerMenu = (
        <CollectorsAdministrationActions selectedSidecarCollectorPairs={selectedSidecarCollectorPairs}
                                         collectors={collectors}
                                         configurations={configurations}
                                         onConfigurationSelectionChange={this.handleConfigurationChange}
                                         onProcessAction={this.handleProcessAction} />
      );
    }

    return (
      <ControlledTableList.Header>
        <HeaderComponentsWrapper>{headerMenu}</HeaderComponentsWrapper>

        <Input ref={(c) => { this.selectAllInput = c; }}
               id="select-all-checkbox"
               type="checkbox"
               label={selectedItems === 0 ? 'Select all' : `${selectedItems} selected`}
               disabled={enabledCollectors.length === 0}
               checked={this.isAllSelected(enabledCollectors, selected)}
               onChange={this.toggleSelectAll}
               wrapperClassName="form-group-inline" />
      </ControlledTableList.Header>
    );
  },

  handleSidecarCollectorSelect(sidecarCollectorId) {
    return (event) => {
      const { selected } = this.state;

      const newSelection = (event.target.checked
        ? lodash.union(selected, [sidecarCollectorId])
        : lodash.without(selected, sidecarCollectorId));

      this.setState({ selected: newSelection });
    };
  },

  isAllSelected(collectors, selected) {
    return collectors.length > 0 && collectors.length === selected.length;
  },

  toggleSelectAll(event) {
    const { enabledCollectors } = this.state;
    const newSelection = (event.target.checked
      ? enabledCollectors.map(({ sidecar, collector }) => this.sidecarCollectorId(sidecar, collector))
      : []);

    this.setState({ selected: newSelection });
  },

  // eslint-disable-next-line react/no-unstable-nested-components
  formatSidecarNoCollectors(sidecar) {
    return (
      <ControlledTableList.Item key={`sidecar-${sidecar.node_id}`}>
        <DisabledCollector className={`${style.collectorEntry} ${style.alignedInformation}`}>
          <Row>
            <Col md={12}>
              <h4 className="list-group-item-heading">
                {sidecar.node_name} <OperatingSystemIcon operatingSystem={sidecar.node_details.operating_system} />
                &emsp;<small>{sidecar.node_id}</small>
              </h4>
            </Col>
          </Row>
          <Row>
            <Col md={12}>
              <span>
                No collectors compatible with {sidecar.node_details.operating_system}
              </span>
            </Col>
          </Row>
        </DisabledCollector>
      </ControlledTableList.Item>
    );
  },

  // eslint-disable-next-line react/no-unstable-nested-components
  formatCollector(sidecar, collector, configurations) {
    const sidecarCollectorId = this.sidecarCollectorId(sidecar, collector);
    const configAssignmentIDs = sidecar.assignments.filter((assignment) => assignment.collector_id === collector.id).map((assignment) => assignment.configuration_id);
    const configAssignments = configurations.filter((config) => configAssignmentIDs.includes(config.id)).sort((c1, c2) => naturalSortIgnoreCase(c1.name, c2.name));
    const { selected } = this.state;
    let collectorStatus = { status: null, message: null, id: null };

    try {
      const result = sidecar.node_details.status.collectors.find((c) => c.collector_id === collector.id);

      if (result) {
        collectorStatus = {
          status: result.status,
          message: result.message,
          id: result.collector_id,
        };
      }
    } catch (e) {
      // Do nothing
    }

    return (
      <Row key={sidecarCollectorId}>
        <Col lg={1} md={2} xs={3}>
          <Input id={`${sidecarCollectorId}-checkbox`}
                 type="checkbox"
                 label={collector.name}
                 checked={selected.includes(sidecarCollectorId)}
                 onChange={this.handleSidecarCollectorSelect(sidecarCollectorId)} />
        </Col>
        <Col lg={1} md={2} xs={3}>
          <span className={style.additionalContent}>
            {(configAssignments.length > 0) && (
              <StatusIndicator status={collectorStatus.status}
                               message={collectorStatus.message}
                               id={collectorStatus.id}
                               lastSeen={sidecar.last_seen} />
            )}
          </span>
        </Col>
        <Col lg={10} md={8} xs={6}>
          <span className={style.additionalContent}>
            {(configAssignments.length > 0) && 
              <IconButton 
                size="sm"
                name="edit"
                onClick={() => this.setState({ selected: [sidecarCollectorId], showConfigurationModal: true })}
              />
            }
            {configAssignments.map((configuration) => 
              <Link 
                key={configuration.id} 
                to={Routes.SYSTEM.SIDECARS.EDIT_CONFIGURATION(configuration.id)}
              >
                <ColorLabel 
                  color={configuration.color} 
                  text={configuration.name} 
                  style={{ display: 'flex' }}
                />
              </Link>
            )}
          </span>
        </Col>
      </Row>
    );
  },

  // eslint-disable-next-line react/no-unstable-nested-components
  formatSidecar(sidecar, collectors, configurations) {
    if (collectors.length === 0) {
      return this.formatSidecarNoCollectors(sidecar);
    }

    return (
      <ControlledTableList.Item key={`sidecar-${sidecar.node_id}`}>
        <div className={style.collectorEntry}>
          <Row>
            <Col md={12}>
              <h4 className={`list-group-item-heading ${style.alignedInformation} ${!sidecar.active && commonStyle.greyedOut}`}>
                {sidecar.node_name} <OperatingSystemIcon operatingSystem={sidecar.node_details.operating_system} />
                &emsp;<small>{sidecar.node_id} {!sidecar.active && <b>&mdash; inactive</b>}</small>
              </h4>
            </Col>
          </Row>
          {collectors.map((collector) => this.formatCollector(sidecar, collector, configurations))}
        </div>
      </ControlledTableList.Item>
    );
  },

  handleSearch(query, callback) {
    const { onQueryChange } = this.props;

    onQueryChange(query, callback());
  },

  handleReset() {
    const { onQueryChange } = this.props;

    onQueryChange();
  },

  handleResetFilters() {
    const { onFilter } = this.props;

    onFilter();
  },

  render() {
    const { configurations, collectors, onPageChange, pagination, query, sidecarCollectorPairs, filters } = this.props;
    const { selected, showConfigurationModal } = this.state;

    const selectedSidecarCollectorPairs = selected.map((selectedSidecarCollectorId) => {
      return sidecarCollectorPairs.find(({ sidecar, collector }) => this.sidecarCollectorId(sidecar, collector) === selectedSidecarCollectorId);
    });

    let formattedCollectors;

    if (sidecarCollectorPairs.length === 0) {
      formattedCollectors = (
        <ControlledTableList.Item>
          {sidecarCollectorPairs.length === 0 ? 'There are no collectors to display' : 'Filters do not match any collectors'}
        </ControlledTableList.Item>
      );
    } else {
      const sidecars = lodash.uniq(sidecarCollectorPairs.map(({ sidecar }) => sidecar));

      formattedCollectors = sidecars.map((sidecarToMap) => {
        const sidecarCollectors = sidecarCollectorPairs
          .filter(({ sidecar }) => sidecar.node_id === sidecarToMap.node_id)
          .map(({ collector }) => collector)
          .filter((collector) => !lodash.isEmpty(collector));

        return this.formatSidecar(sidecarToMap, sidecarCollectors, configurations);
      });
    }

    return (
      <div className={style.paginatedList}>
        <PaginatedList pageSizes={PAGE_SIZES}
                       totalItems={pagination.total}
                       onChange={onPageChange}>
          <SidecarSearchForm query={query} onSearch={this.handleSearch} onReset={this.handleReset} />
          <FiltersSummary collectors={collectors}
                          configurations={configurations}
                          filters={filters}
                          onResetFilters={this.handleResetFilters} />
          <Row>
            <Col md={12}>
              <ControlledTableList>
                {this.formatHeader(selectedSidecarCollectorPairs)}
                {formattedCollectors}
              </ControlledTableList>
            </Col>
          </Row>
        </PaginatedList>
        <CollectorConfigurationModalContainer collectors={collectors}
                                              configurations={configurations}
                                              selectedSidecarCollectorPairs={selectedSidecarCollectorPairs}
                                              onConfigurationSelectionChange={this.handleConfigurationChange}
                                              show={showConfigurationModal}
                                              onCancel={() => this.setState({ selected: [], showConfigurationModal: false })} />
      </div>
    );
  },
});

export default CollectorsAdministration;
