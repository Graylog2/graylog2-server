// @flow strict
import * as React from 'react';
import styled from 'styled-components';

import type { PaginatedEnititySharesType } from 'actions/permissions/EntityShareActions';
import mockedPermissions from 'logic/permissions/mocked';
import { SearchForm, Select } from 'components/common';

import SharedEntitiesQueryHelper from './SharedEntitiesQueryHelper';

type Props = {
  onSearch: (query: string) => Promise<?PaginatedEnititySharesType>,
  onReset: () => Promise<?PaginatedEnititySharesType>,
  onFilter: (param: string, value: string) => Promise<?PaginatedEnititySharesType>,
};

const StyledSearchForm = styled(SearchForm)`
  display: inline-block;

  margin-bottom: 10px;
  margin-right: 15px;
`;

const Filters = styled.div`
  display: inline-block;
  vertical-align: top;

  margin-right: 15px;
  margin-bottom: 10px;
`;

const SelectWrapper = styled.div`
  display: inline-flex;
  align-items: center;
  vertical-align: top;

  width: 260px;
  margin-left: 10px;
  margin-right: 10px;

  white-space: nowrap;
`;

const StyledSelect = styled(Select)`
  width: 300px;
  margin-left: 10px;
`;

const entityTypeOptions = Object.entries(mockedPermissions.availabeEntityTypes).map(([key, value]) => ({ label: value, value: key }));
const capabilityOptions = Object.entries(mockedPermissions.availableCapabilities).map(([key, value]) => ({ label: value, value: key }));

const SharedEntitiesFilter = ({ onSearch, onFilter, onReset }: Props) => {
  const _handleSearch = (newQuery, resetLoading) => onSearch(newQuery).then(resetLoading);

  return (
    <>
      <StyledSearchForm onReset={onReset}
                        onSearch={_handleSearch}
                        queryHelpComponent={<SharedEntitiesQueryHelper />}
                        topMargin={0}
                        useLoadingState />

      <Filters>
        <SelectWrapper>
          <label htmlFor="entity-type-select">Entity Type</label>
          <StyledSelect inputId="entity-type-select"
                        onChange={(entityType) => onFilter('entity_type', entityType)}
                        options={entityTypeOptions}
                        placeholder="Filter entity types" />
        </SelectWrapper>
        <SelectWrapper>
          <label htmlFor="capability-select">Capability</label>
          <StyledSelect inputId="capability-select"
                        onChange={(capability) => onFilter('capability', capability)}
                        options={capabilityOptions}
                        placeholder="Filter capabilies" />
        </SelectWrapper>
      </Filters>
    </>
  );
};

export default SharedEntitiesFilter;
