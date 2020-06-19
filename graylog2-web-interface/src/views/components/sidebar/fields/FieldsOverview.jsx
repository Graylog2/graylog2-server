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
  listHeight: number,
  fields: ImmutableList<FieldTypeMapping>,
  allFields: ImmutableList<FieldTypeMapping>,
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

const FieldsOverview = ({ allFields, fields, viewMetadata, listHeight }: Props) => {
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
      <List viewMetadata={viewMetadata}
            listHeight={listHeight}
            filter={filter}
            fields={fields}
            allFields={allFields}
            currentGroup={currentGroup} />
    </Container>
  );
};

FieldsOverview.propTypes = {
  allFields: PropTypes.object.isRequired,
  listHeight: PropTypes.number,
  fields: PropTypes.object.isRequired,
};

FieldsOverview.defaultProps = {
  listHeight: 50,
};

export default FieldsOverview;
