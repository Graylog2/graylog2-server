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
import ChangeMode from './ChangeMode';

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

const FieldListModes = styled.div`
  margin-top: 5px;
  margin-bottom: 0;
`;

const reducer = (state, action: {type: string, payload?: { mode?: string, filter?: string }}): { filter: ?string, currentMode: string } => {
  const { payload = {} } = action;
  const { mode, filter } = payload;
  switch (action.type) {
    case 'search':
      return { ...state, filter: filter };
    case 'searchReset':
      return { ...state, filter: undefined };
    case 'changeMode':
      return mode ? { ...state, currentMode: mode } : state;
    default:
      return state;
  }
};

const FieldList = ({ allFields, fields, viewMetadata, listHeight }: Props) => {
  const [{ filter, currentMode }, dispatch] = useReducer(reducer, { filter: undefined, currentMode: 'current' });
  const handleSearch = (e) => dispatch({ type: 'search', payload: { filter: e.target.value } });
  const changeMode = (mode) => dispatch({ type: 'changeMode', payload: { mode } });
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
      <FieldListModes>
        List fields of{' '}
        <ChangeMode changeMode={changeMode}
                    currentMode={currentMode}
                    mode="current"
                    text="current streams"
                    title="This shows fields which are (prospectively) included in the streams you have selected." />
        {', '}
        <ChangeMode changeMode={changeMode}
                    currentMode={currentMode}
                    mode="all"
                    text="all"
                    title="This shows all fields, but no reserved (gl2_*) fields." />
        {' or '}
        <ChangeMode changeMode={changeMode}
                    currentMode={currentMode}
                    mode="allreserved"
                    text="all including reserved"
                    title="This shows all fields, including reserved (gl2_*) fields." />
        {' fields.'}
      </FieldListModes>
      <hr />
      <List viewMetadata={viewMetadata}
            listHeight={listHeight}
            filter={filter}
            fields={fields}
            allFields={allFields}
            currentMode={currentMode} />
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
