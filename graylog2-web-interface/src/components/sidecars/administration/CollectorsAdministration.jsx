import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Col, Row } from 'components/graylog';
import { Link } from 'react-router';
import Routes from 'routing/Routes';

import { ControlledTableList, PaginatedList } from 'components/common';
import { Input } from 'components/bootstrap';

import ColorLabel from 'components/sidecars/common/ColorLabel';
import OperatingSystemIcon from 'components/sidecars/common/OperatingSystemIcon';
import SidecarSearchForm from 'components/sidecars/common/SidecarSearchForm';
import StatusIndicator from 'components/sidecars/common/StatusIndicator';

import commonStyle from 'components/sidecars/common/CommonSidecarStyles.css';

import CollectorsAdministrationActions from './CollectorsAdministrationActions';
import CollectorsAdministrationFilters from './CollectorsAdministrationFilters';
import FiltersSummary from './FiltersSummary';

import style from './CollectorsAdministration.css';

const CollectorsAdministration = createReactClass({
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
    };
  },

  componentWillReceiveProps(nextProps) {
    if (!lodash.isEqual(this.props.sidecarCollectorPairs, nextProps.sidecarCollectorPairs)) {
      this.setState({
        enabledCollectors: this.getEnabledCollectors(nextProps.sidecarCollectorPairs),
        selected: this.filterSelectedCollectors(nextProps.sidecarCollectorPairs),
      });
    }
  },

  componentDidUpdate() {
    this.setSelectAllCheckboxState(this.selectAllInput, this.state.enabledCollectors, this.state.selected);
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
    const filteredSidecarCollectorIds = collectors.map(({ collector, sidecar }) => this.sidecarCollectorId(sidecar, collector));
    return this.state.selected.filter((sidecarCollectorId) => filteredSidecarCollectorIds.includes(sidecarCollectorId));
  },

  handleConfigurationChange(selectedConfigurations, doneCallback) {
    const { selected, enabledCollectors } = this.state;

    const selectedSidecars = enabledCollectors
      .filter(({ sidecar, collector }) => selected.includes(this.sidecarCollectorId(sidecar, collector)));

    this.props.onConfigurationChange(selectedSidecars, selectedConfigurations, doneCallback);
  },

  handleProcessAction(action, selectedSidecarCollectorPairs, doneCallback) {
    const selectedCollectors = {};
    selectedSidecarCollectorPairs.forEach(({ sidecar, collector }) => {
      if (selectedCollectors[sidecar.node_id]) {
        selectedCollectors[sidecar.node_id].push(collector.id);
      } else {
        selectedCollectors[sidecar.node_id] = [collector.id];
      }
    });
    this.props.onProcessAction(action, selectedCollectors, doneCallback);
  },

  formatHeader() {
    const { collectors, configurations, sidecarCollectorPairs } = this.props;
    const { selected, enabledCollectors } = this.state;
    const selectedItems = this.state.selected.length;

    const selectedSidecarCollectorPairs = selected.map((selectedSidecarCollectorId) => {
      return sidecarCollectorPairs.find(({ sidecar, collector }) => this.sidecarCollectorId(sidecar, collector) === selectedSidecarCollectorId);
    });

    let headerMenu;
    if (selectedItems === 0) {
      headerMenu = (
        <CollectorsAdministrationFilters collectors={collectors} configurations={configurations} filters={this.props.filters} filter={this.props.onFilter} />
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
        <div className={style.headerComponentsWrapper}>{headerMenu}</div>

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
      const newSelection = (event.target.checked
        ? lodash.union(this.state.selected, [sidecarCollectorId])
        : lodash.without(this.state.selected, sidecarCollectorId));
      this.setState({ selected: newSelection });
    };
  },

  isAllSelected(collectors, selected) {
    return collectors.length > 0 && collectors.length === selected.length;
  },

  toggleSelectAll(event) {
    const newSelection = (event.target.checked
      ? this.state.enabledCollectors.map(({ sidecar, collector }) => this.sidecarCollectorId(sidecar, collector))
      : []);
    this.setState({ selected: newSelection });
  },

  formatSidecarNoCollectors(sidecar) {
    return (
      <ControlledTableList.Item key={`sidecar-${sidecar.node_id}`}>
        <div className={`${style.collectorEntry} ${style.disabledCollector} ${style.alignedInformation}`}>
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
        </div>
      </ControlledTableList.Item>
    );
  },

  formatCollector(sidecar, collector, configurations) {
    const sidecarCollectorId = this.sidecarCollectorId(sidecar, collector);
    const configAssignment = sidecar.assignments.find((assignment) => assignment.collector_id === collector.id) || {};
    const configuration = configurations.find((config) => config.id === configAssignment.configuration_id);
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
        <Col lg={2} md={4} xs={6}>
          <Input id={`${sidecarCollectorId}-checkbox`}
                 type="checkbox"
                 label={collector.name}
                 checked={this.state.selected.includes(sidecarCollectorId)}
                 onChange={this.handleSidecarCollectorSelect(sidecarCollectorId)} />
        </Col>
        <Col lg={1} md={2} xs={3}>
          <span className={style.additionalContent}>
            {configuration && (
              <StatusIndicator status={collectorStatus.status}
                               message={collectorStatus.message}
                               id={collectorStatus.id}
                               lastSeen={sidecar.last_seen} />
            )}
          </span>
        </Col>
        <Col lg={1} md={2} xs={3}>
          <span className={style.additionalContent}>
            {configuration && <Link to={Routes.SYSTEM.SIDECARS.EDIT_CONFIGURATION(configuration.id)}><ColorLabel color={configuration.color} text={configuration.name} />  </Link>}
          </span>
        </Col>
      </Row>
    );
  },

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
    this.props.onQueryChange(query, callback());
  },

  handleReset() {
    this.props.onQueryChange();
  },

  handleResetFilters() {
    this.props.onFilter();
  },

  render() {
    const { configurations, collectors, onPageChange, pagination, query, sidecarCollectorPairs, filters } = this.props;

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
        <PaginatedList activePage={pagination.page}
                       pageSize={pagination.pageSize}
                       pageSizes={[10, 25, 50, 100]}
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
                {this.formatHeader()}
                {formattedCollectors}
              </ControlledTableList>
            </Col>
          </Row>
        </PaginatedList>
      </div>
    );
  },
});

export default CollectorsAdministration;
