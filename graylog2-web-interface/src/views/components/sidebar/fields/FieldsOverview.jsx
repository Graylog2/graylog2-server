// @flow strict
import * as React from 'react';
import { useState } from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent } from 'styled-components';
import { List as ImmutableList } from 'immutable';

import connect from 'stores/connect';
import type { ViewMetaData as ViewMetadata } from 'views/stores/ViewMetadataStore';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { type ThemeInterface } from 'theme';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import { Button } from 'components/graylog';

import List from './List';
import FieldGroup from './FieldGroup';

type Props = {
  activeQueryFields: ImmutableList<FieldTypeMapping>,
  allFields: ImmutableList<FieldTypeMapping>,
  listHeight: number,
  viewMetadata: ViewMetadata,
};

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  white-space: break-spaces;
`;

const FilterForm = styled.form`
  display: flex;
`;

const FilterInputWrapper = styled.div`
  flex-grow: 1;
  margin-right: 5px;
`;

const FilterInput = styled.input`
  width: 100%;
`;

const FieldListGroups = styled.div`
  margin-top: 5px;
  margin-bottom: 0;
`;

const FieldsOverview = ({ allFields, activeQueryFields, viewMetadata, listHeight }: Props) => {
  const [currentGroup, setCurrentGroup] = useState('current');
  const [filter, setFilter] = useState(undefined);
  const handleSearch = (e) => setFilter(e.target.value);
  const handleSearchReset = () => setFilter(undefined);
  return (
    <Container>
      <FilterForm className="form-inline" onSubmit={(e) => e.preventDefault()}>
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
      <FieldListGroups>
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
      </FieldListGroups>
      <hr />
      <List activeQueryFields={activeQueryFields}
            allFields={allFields}
            currentGroup={currentGroup}
            filter={filter}
            listHeight={listHeight}
            viewMetadata={viewMetadata} />
    </Container>
  );
};

const FieldListWithContext = (props) => (
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

FieldListWithContext.propTypes = {
  viewMetadata: PropTypes.object.isRequired,
  listHeight: PropTypes.number,
};

FieldListWithContext.defaultProps = {
  listHeight: 50,
};

export default connect(FieldListWithContext, { viewMetadata: ViewMetadataStore });
