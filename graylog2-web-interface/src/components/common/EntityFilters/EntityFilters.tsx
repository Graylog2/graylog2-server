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
import React, { useCallback } from 'react';
import styled from 'styled-components';
import { OrderedMap } from 'immutable';

import CreateFilterDropdown from 'components/common/EntityFilters/CreateFilterDropdown';
import type { Attributes } from 'stores/PaginationTypes';
import type { Filters, Filter, UrlQueryFilters } from 'components/common/EntityFilters/types';
import ActiveFilters from 'components/common/EntityFilters/ActiveFilters';
import useFiltersWithTitle from 'components/common/EntityFilters/hooks/useFiltersWithTitle';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';

import { ROW_MIN_HEIGHT } from './Constants';

const SUPPORTED_ATTRIBUTE_TYPES = ['STRING', 'BOOLEAN', 'DATE', 'OBJECT_ID'];

const FilterCreation = styled.div`
  display: inline-flex;
  height: ${ROW_MIN_HEIGHT}px;
  align-items: center;
  margin-left: 5px;
  
  && {
    margin-right: 10px;
  }
`;

type Props = {
  attributes?: Attributes,
  urlQueryFilters: UrlQueryFilters | undefined,
  setUrlQueryFilters: (urlQueryFilters: UrlQueryFilters) => void,
  filterValueRenderers?: { [attributeId: string]: (value: Filter['value'], title: string) => React.ReactNode },
  appSection: string,
}

const EntityFilters = ({ attributes = [], filterValueRenderers = undefined, urlQueryFilters, setUrlQueryFilters, appSection }: Props) => {
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  const {
    data: activeFilters,
    onChange: onChangeFiltersWithTitle,
  } = useFiltersWithTitle(
    urlQueryFilters,
    attributes,
    !!attributes,
  );

  const filterableAttributes = attributes.filter(({ filterable, type }) => filterable && SUPPORTED_ATTRIBUTE_TYPES.includes(type));

  const onChangeFilters = useCallback((newFilters: Filters) => {
    const newUrlQueryFilters = newFilters.entrySeq().reduce((col, [attributeId, filterCol]) => (
      col.set(attributeId, [...(col.get(attributeId) ?? []), ...filterCol.map(({ value }) => value)])
    ), OrderedMap<string, Array<string>>());

    onChangeFiltersWithTitle(newFilters, newUrlQueryFilters);
    setUrlQueryFilters(newUrlQueryFilters);
  }, [onChangeFiltersWithTitle, setUrlQueryFilters]);

  const onCreateFilter = useCallback((attributeId: string, filter: Filter) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.FILTER_CREATED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: appSection,
      app_action_value: 'filter-created',
      attribute_id: attributeId,
    });

    onChangeFilters(OrderedMap(activeFilters).set(
      attributeId,
      [...(activeFilters?.get(attributeId) ?? []), filter],
    ));
  }, [activeFilters, appSection, onChangeFilters, pathname, sendTelemetry]);

  const onDeleteFilter = useCallback((attributeId: string, filterId: string) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.FILTER_DELETED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: appSection,
      app_action_value: 'filter-deleted',
      attribute_id: attributeId,
    });

    const filterGroup = activeFilters.get(attributeId);
    const updatedFilterGroup = filterGroup.filter(({ value }) => value !== filterId);

    if (updatedFilterGroup.length) {
      return onChangeFilters(activeFilters.set(attributeId, updatedFilterGroup));
    }

    return onChangeFilters(activeFilters.remove(attributeId));
  }, [activeFilters, appSection, onChangeFilters, pathname, sendTelemetry]);

  const onChangeFilter = useCallback((attributeId: string, prevValue: string, newFilter: Filter) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.ENTITY_DATA_TABLE.FILTER_CHANGED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: appSection,
      app_action_value: 'filter-value-changed',
      attribute_id: attributeId,
    });

    const filterGroup = activeFilters.get(attributeId);
    const targetFilterIndex = filterGroup.findIndex(({ value }) => value === prevValue);
    const updatedFilterGroup = [...filterGroup];
    updatedFilterGroup[targetFilterIndex] = newFilter;

    onChangeFilters(activeFilters.set(attributeId, updatedFilterGroup));
  }, [activeFilters, appSection, onChangeFilters, pathname, sendTelemetry]);

  if (!filterableAttributes.length) {
    return null;
  }

  return (
    <>
      <FilterCreation>
        Filters
        <CreateFilterDropdown filterableAttributes={filterableAttributes}
                              onCreateFilter={onCreateFilter}
                              activeFilters={activeFilters}
                              filterValueRenderers={filterValueRenderers} />

      </FilterCreation>
      {activeFilters && (
        <ActiveFilters filters={activeFilters}
                       attributes={attributes}
                       onChangeFilter={onChangeFilter}
                       onDeleteFilter={onDeleteFilter}
                       filterValueRenderers={filterValueRenderers} />
      )}
    </>
  );
};

export default EntityFilters;
