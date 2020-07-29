// @flow strict
import * as React from 'react';
import styled from 'styled-components';

import mockedPermissions from 'logic/permissions/mocked';
import { SearchForm, Select } from 'components/common';

type Props = {
  onSearch: (query: string, resetLoading: () => void) => Promise<void>,
  onReset: () => Promise<void>,
  onFilter: (param: string, value: string) => Promise<void>,
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

const SharedEntitiesFilter = ({ onSearch, onFilter, onReset }: Props) => (
  <>
    <StyledSearchForm onReset={onReset}
                      onSearch={onSearch}
                      placeholder="Filter by name"
                      topMargin={0}
                      useLoadingState />

    <Filters>
      <SelectWrapper>
        Entity types
        <StyledSelect onChange={(entityType) => onFilter('entity_type', entityType)}
                      options={entityTypeOptions}
                      placeholder="Filter entity types" />
      </SelectWrapper>
      <SelectWrapper>
        Capability
        <StyledSelect onChange={(capability) => onFilter('capability', capability)}
                      options={capabilityOptions}
                      placeholder="Filter capabilies" />
      </SelectWrapper>
    </Filters>
  </>
);

export default SharedEntitiesFilter;
