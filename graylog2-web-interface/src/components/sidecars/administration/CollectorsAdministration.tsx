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
import React, { useState, useRef, useEffect } from 'react';
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
import type { Pagination } from 'views/stores/DashboardsStore';

import CollectorsAdministrationActions from './CollectorsAdministrationActions';
import CollectorsAdministrationFilters from './CollectorsAdministrationFilters';
import CollectorConfigurationModalContainer from './CollectorConfigurationModalContainer';
import FiltersSummary from './FiltersSummary';
import style from './CollectorsAdministration.css';

import type { Collector, Configuration, SidecarCollectorPairType, SidecarSummary } from '../types';

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

type Props = {
  collectors: Collector[],
  configurations: Configuration[],
  sidecarCollectorPairs: SidecarCollectorPairType[],
  query: string,
  filters: { [_key: string]: string },
  pagination: Pagination,
  onPageChange: (currentPage: number, pageSize: number) => void,
  onFilter: (collectorIds?: string[], callback?: () => void) => void,
  onQueryChange: (query?: string, callback?: () => void) => void,
  onConfigurationChange: (pairs: SidecarCollectorPairType[], configs: Configuration[], callback: () => void) => void,
  onProcessAction: (action: string, collectorDict: { [sidecarId: string]: string[] }, callback: () => void) => void,
};

const CollectorsAdministration = ({
  configurations,
  collectors,
  onPageChange,
  pagination,
  query,
  sidecarCollectorPairs,
  filters,
  onFilter,
  onQueryChange,
  onConfigurationChange,
  onProcessAction,
}: Props) => {
  const [showConfigurationModal, setShowConfigurationModal] = useState(false);
  const [selected, setSelected] = useState<string[]>([]);
  const selectAllInput = useRef(null);

  // Filter out sidecars with no compatible collectors
  const enabledCollectors = sidecarCollectorPairs.filter(({ collector }) => !lodash.isEmpty(collector));

  const sidecarCollectorId = (sidecar: SidecarSummary, collector: Collector) => {
    return `${sidecar.node_id}-${collector.name}`;
  };

  const isAllSelected = (_collectors: (SidecarCollectorPairType|Collector)[], _selected: string[]) => {
    return _collectors.length > 0 && _collectors.length === _selected.length;
  };

  useEffect(() => {
    const selectAllCheckbox = selectAllInput ? selectAllInput.current.getInputDOMNode() : undefined;

    if (selectAllCheckbox) {
      // Set the select all checkbox as indeterminate if some but not all items are selected.
      selectAllCheckbox.indeterminate = selected.length > 0 && !isAllSelected(collectors, selected);
    }
  }, [selectAllInput, collectors, selected]);

  const handleConfigurationChange = (selectedSidecars: SidecarCollectorPairType[], selectedConfigurations: Configuration[], doneCallback: () => void) => {
    onConfigurationChange(selectedSidecars, selectedConfigurations, doneCallback);
  };

  const handleProcessAction = (action: string, selectedSidecarCollectorPairs: SidecarCollectorPairType[], doneCallback: () => void) => {
    const selectedCollectors = {};

    selectedSidecarCollectorPairs.forEach(({ sidecar, collector }) => {
      if (selectedCollectors[sidecar.node_id]) {
        selectedCollectors[sidecar.node_id].push(collector.id);
      } else {
        selectedCollectors[sidecar.node_id] = [collector.id];
      }
    });

    onProcessAction(action, selectedCollectors, doneCallback);
  };

  const toggleSelectAll = (event) => {
    const newSelection = (event.target.checked
      ? enabledCollectors.map(({ sidecar, collector }) => sidecarCollectorId(sidecar, collector))
      : []);

    setSelected(newSelection);
  };

  const formatHeader = (selectedSidecarCollectorPairs: SidecarCollectorPairType[]) => {
    const selectedItems = selected.length;

    let headerMenu;

    if (selectedItems === 0) {
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
                                         onConfigurationSelectionChange={handleConfigurationChange}
                                         onProcessAction={handleProcessAction} />
      );
    }

    return (
      <ControlledTableList.Header>
        <HeaderComponentsWrapper>{headerMenu}</HeaderComponentsWrapper>

        <Input ref={selectAllInput}
               id="select-all-checkbox"
               type="checkbox"
               label={selectedItems === 0 ? 'Select all' : `${selectedItems} selected`}
               disabled={enabledCollectors.length === 0}
               checked={isAllSelected(enabledCollectors, selected)}
               onChange={toggleSelectAll}
               wrapperClassName="form-group-inline" />
      </ControlledTableList.Header>
    );
  };

  const handleSidecarCollectorSelect = (collectorId: string) => {
    return (event) => {
      const newSelection = (event.target.checked
        ? lodash.union(selected, [collectorId])
        : lodash.without(selected, collectorId));

      setSelected(newSelection);
    };
  };

  const formatSidecarNoCollectors = (sidecar: SidecarSummary) => {
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
  };

  const formatCollector = (sidecar: SidecarSummary, collector: Collector, _configurations: Configuration[]) => {
    const collectorId = sidecarCollectorId(sidecar, collector);
    const configAssignmentIDs = sidecar.assignments.filter((assignment) => assignment.collector_id === collector.id).map((assignment) => assignment.configuration_id);
    const configAssignments = _configurations.filter((config) => configAssignmentIDs.includes(config.id)).sort((c1, c2) => naturalSortIgnoreCase(c1.name, c2.name));
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
      <Row key={collectorId}>
        <Col lg={1} md={2} xs={3}>
          <Input id={`${collectorId}-checkbox`}
                 type="checkbox"
                 label={collector.name}
                 checked={selected.includes(collectorId)}
                 onChange={handleSidecarCollectorSelect(collectorId)} />
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
            {(configAssignments.length > 0)
              && (
              <IconButton name="edit"
                          onClick={() => {
                            setSelected([collectorId]);
                            setShowConfigurationModal(true);
                          }} />
              )}
            {configAssignments.map((configuration) => (
              <Link key={configuration.id}
                    to={Routes.SYSTEM.SIDECARS.EDIT_CONFIGURATION(configuration.id)}>
                <ColorLabel color={configuration.color}
                            text={configuration.name}
                            className="flex-color-label" />
              </Link>
            ),
            )}
          </span>
        </Col>
      </Row>
    );
  };

  const formatSidecar = (sidecar: SidecarSummary, _collectors: Collector[], _configurations: Configuration[]) => {
    if (_collectors.length === 0) {
      return formatSidecarNoCollectors(sidecar);
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
          {_collectors.map((collector) => formatCollector(sidecar, collector, _configurations))}
        </div>
      </ControlledTableList.Item>
    );
  };

  const handleSearch = (_query: string, callback: () => void) => {
    onQueryChange(_query, callback);
  };

  const handleReset = () => {
    onQueryChange();
  };

  const handleResetFilters = () => {
    onFilter();
  };

  const selectedSidecarCollectorPairs = selected.map((selectedSidecarCollectorId) => {
    return sidecarCollectorPairs.find(({ sidecar, collector }) => sidecarCollectorId(sidecar, collector) === selectedSidecarCollectorId);
  });

  let formattedCollectors;

  if (sidecarCollectorPairs.length === 0) {
    formattedCollectors = (
      <ControlledTableList.Item>
        {sidecarCollectorPairs.length === 0 ? 'There are no collectors to display' : 'Filters do not match any collectors'}
      </ControlledTableList.Item>
    );
  } else {
    const sidecars = lodash.uniq<SidecarSummary>(sidecarCollectorPairs.map(({ sidecar }) => sidecar));

    formattedCollectors = sidecars.map((sidecarToMap) => {
      const sidecarCollectors = sidecarCollectorPairs
        .filter(({ sidecar }) => sidecar.node_id === sidecarToMap.node_id)
        .map(({ collector }) => collector)
        .filter((collector) => !lodash.isEmpty(collector));

      return formatSidecar(sidecarToMap, sidecarCollectors, configurations);
    });
  }

  return (
    <div className={style.paginatedList}>
      <PaginatedList pageSizes={PAGE_SIZES}
                     totalItems={pagination.total}
                     onChange={onPageChange}>
        <SidecarSearchForm query={query} onSearch={handleSearch} onReset={handleReset} />
        <FiltersSummary collectors={collectors}
                        configurations={configurations}
                        filters={filters}
                        onResetFilters={handleResetFilters} />
        <Row>
          <Col md={12}>
            <ControlledTableList>
              {formatHeader(selectedSidecarCollectorPairs)}
              {formattedCollectors}
            </ControlledTableList>
          </Col>
        </Row>
      </PaginatedList>
      <CollectorConfigurationModalContainer collectors={collectors}
                                            configurations={configurations}
                                            selectedSidecarCollectorPairs={selectedSidecarCollectorPairs}
                                            onConfigurationSelectionChange={handleConfigurationChange}
                                            show={showConfigurationModal}
                                            onCancel={() => {
                                              setSelected([]);
                                              setShowConfigurationModal(false);
                                            }} />
    </div>
  );
};

CollectorsAdministration.propTypes = {
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
};

export default CollectorsAdministration;
