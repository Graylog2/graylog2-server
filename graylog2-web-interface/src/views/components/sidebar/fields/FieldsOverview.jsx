// @flow strict
import * as React from 'react';
import { useState } from 'react';
import PropTypes from 'prop-types';
import { List as ImmutableList } from 'immutable';
import styled, { type StyledComponent } from 'styled-components';

import type { ViewMetaData as ViewMetadata } from 'views/stores/ViewMetadataStore';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { type ThemeInterface } from 'theme';

import { Button } from 'components/graylog';
import List from './List';
import FieldGroup from './FieldGroup';

type Props = {
  viewMetadata: ViewMetadata,
  fields: ImmutableList<FieldTypeMapping>,
  allFields: ImmutableList<FieldTypeMapping>,
};

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  white-space: break-spaces;
  height: 100%;
  display: grid;
  display: -ms-grid;
  grid-template-columns: 1fr;
  grid-template-rows: auto 1fr;
  -ms-grid-columns: 1fr;
  -ms-grid-rows: auto 1fr;

  > *:nth-child(1) {
    grid-column: 1;
    -ms-grid-column: 1;
    grid-row: 1;
    -ms-grid-row: 1;
  }
  > *:nth-child(2) {
    grid-column: 1;
    -ms-grid-column: 1;
    grid-row: 2;
    -ms-grid-row: 2;
  }
`;

const FilterForm = styled.form`
  display: flex;
`;

const FilterInputWrapper = styled.div`
  margin-right: 5px;
`;

const FilterInput = styled.input`
  width: 100%;
`;

const FieldGroups = styled.div`
  margin-top: 5px;
  margin-bottom: 0;
`;

const FieldsOverview = ({ allFields, fields, viewMetadata }: Props) => {
  const [currentGroup, setCurrentGroup] = useState('current');
  const [filter, setFilter] = useState(undefined);
  const handleSearch = (e) => setFilter(e.target.value);
  const handleSearchReset = () => setFilter(undefined);
  return (
    <Container>
      <div>
        <FilterForm onSubmit={(e) => e.preventDefault()}>
          <FilterInputWrapper className="form-group has-feedback">
            <FilterInput id="common-search-form-query-input"
                         className="query form-control"
                         onChange={handleSearch}
                         value={filter || ''}
                         placeholder="Filter fields"
                         type="text"
                         autoComplete="off"
                         spellCheck="false" />
          </FilterInputWrapper>
          <div className="form-group">
            <Button type="reset" className="reset-button" onClick={handleSearchReset}>
              Reset
            </Button>
          </div>
        </FilterForm>
        <FieldGroups>
          List fields of{' '}
          <FieldGroup selected={currentGroup === 'current'}
                      group="current"
                      text="current streams"
                      title="This shows fields which are (prospectively) included in the streams you have selected."
                      onSelect={setCurrentGroup} />
          {', '}
          <FieldGroup selected={currentGroup === 'all'}
                      group="all"
                      text="all"
                      title="This shows all fields, but no reserved (gl2_*) fields."
                      onSelect={setCurrentGroup} />
          {' or '}
          <FieldGroup onSelect={setCurrentGroup}
                      selected={currentGroup === 'allreserved'}
                      group="allreserved"
                      text="all including reserved"
                      title="This shows all fields, including reserved (gl2_*) fields." />
          {' fields.'}
        </FieldGroups>
        <hr />
      </div>
      <List viewMetadata={viewMetadata}
            filter={filter}
            fields={fields}
            allFields={allFields}
            currentGroup={currentGroup} />
    </Container>
  );
};

FieldsOverview.propTypes = {
  allFields: PropTypes.object.isRequired,
  fields: PropTypes.object.isRequired,
};


export default FieldsOverview;
