// @flow strict
import * as React from 'react';
import { useReducer } from 'react';
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

const reducer = (state, action: {type: string, payload?: { fieldGroup?: string, filter?: string }}): { filter: ?string, currentGroup: string } => {
  const { payload = {} } = action;
  const { fieldGroup, filter } = payload;
  switch (action.type) {
    case 'search':
      return { ...state, filter: filter };
    case 'searchReset':
      return { ...state, filter: undefined };
    case 'changeGroup':
      return fieldGroup ? { ...state, currentGroup: fieldGroup } : state;
    default:
      return state;
  }
};

const FieldList = ({ allFields, fields, viewMetadata, listHeight }: Props) => {
  const [{ filter, currentGroup }, dispatch] = useReducer(reducer, { filter: undefined, currentGroup: 'current' });
  const handleSearch = (e) => dispatch({ type: 'search', payload: { filter: e.target.value } });
  const changeFieldGroup = (fieldGroup) => dispatch({ type: 'changeGroup', payload: { fieldGroup } });
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
          <Button type="reset" className="reset-button" onClick={() => dispatch({ type: 'searchReset' })}>
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
                    onSelect={changeFieldGroup} />
        {', '}
        <FieldGroup selected={currentGroup === 'all'}
                    group="all"
                    text="all"
                    title="This shows all fields, but no reserved (gl2_*) fields."
                    onSelect={changeFieldGroup} />
        {' or '}
        <FieldGroup onSelect={changeFieldGroup}
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

FieldList.propTypes = {
  allFields: PropTypes.object.isRequired,
  listHeight: PropTypes.number,
  fields: PropTypes.object.isRequired,
};

FieldList.defaultProps = {
  listHeight: 50,
};

export default FieldList;
