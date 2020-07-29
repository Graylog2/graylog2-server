// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import { SearchForm, Select } from 'components/common';

type Props = {
  onSearch: (query: string, resetLoading: () => void) => Promise<void>,
  onReset: () => Promise<void>,
  onFilter: (param: string, value: string) => Promise<void>,
};

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  
`;

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
  top: 0;
  margin-left: 10px;
  vertical-align: top;
  align-items: center;
  white-space: nowrap;
  width: 260px;
  margin-right: 10px;
`;

const StyledSelect = styled(Select)`
  width: 300px;
  margin-left: 10px;
`;

const entityTypeOptions = [
  { label: 'Stream', value: 'stream' },
  { label: 'Dashboard', value: 'dashboard' },
  { label: 'Saved Search', value: 'saved_search' },
];

const capabilityOptions = [
  { label: 'Owner', value: 'own' },
  { label: 'Viewer', value: 'view' },
  { label: 'Manager', value: 'manage' },
];

const SharedEntitiesFilter = ({ onSearch, onFilter, onReset }: Props) => {
  return (
    <Container>
      <StyledSearchForm onSearch={onSearch}
                        onReset={onReset}
                        placeholder="Filter by name"
                        useLoadingState
                        topMargin={0} />
      <Filters>

        <SelectWrapper>
          Entity types
          <StyledSelect placeholder="Filter entity types"
                        onChange={(entityType) => onFilter('entity_type', entityType)}
                        options={entityTypeOptions} />
        </SelectWrapper>
        <SelectWrapper>
          Capability
          <StyledSelect placeholder="Filter capabilies"
                        onChange={(capability) => onFilter('capability', capability)}
                        options={capabilityOptions} />
        </SelectWrapper>
      </Filters>
    </Container>
  );
};

export default SharedEntitiesFilter;
