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
// @flow strict
import * as React from 'react';
import { useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import type { StyledComponent } from 'styled-components';
import { List as ImmutableList } from 'immutable';

import connect from 'stores/connect';
import type { ViewMetaData as ViewMetadata } from 'views/stores/ViewMetadataStore';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import type { ThemeInterface } from 'theme';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import { Button } from 'components/graylog';

import List from './List';
import FieldGroup from './FieldGroup';

type Props = {
  activeQueryFields: ImmutableList<FieldTypeMapping>,
  allFields: ImmutableList<FieldTypeMapping>,
  viewMetadata: ViewMetadata,
};

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  white-space: break-spaces;
  height: 100%;
  display: grid;
  display: -ms-grid;
  grid-template-columns: 1fr;
  grid-template-rows: max-content 1fr;
  -ms-grid-columns: 1fr;
  -ms-grid-rows: max-content 1fr;

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

const FieldsOverview = ({ allFields, activeQueryFields, viewMetadata }: Props) => {
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
            activeQueryFields={activeQueryFields}
            allFields={allFields}
            currentGroup={currentGroup} />
    </Container>
  );
};

const FieldsOverviewWithContext = (props) => (
  <FieldTypesContext.Consumer>
    {(fieldTypes) => {
      const { viewMetadata: { activeQuery } } = props;
      const allFields = fieldTypes?.all;
      const queryFields = fieldTypes?.queryFields;
      const activeQueryFields = queryFields?.get(activeQuery, allFields);

      return <FieldsOverview {...props} allFields={allFields} activeQueryFields={activeQueryFields} />;
    }}
  </FieldTypesContext.Consumer>
);

FieldsOverviewWithContext.propTypes = {
  viewMetadata: PropTypes.object.isRequired,
};

export default connect(FieldsOverviewWithContext, { viewMetadata: ViewMetadataStore });
