import React, { useRef } from 'react';
import styled from 'styled-components';

import { Button } from 'components/graylog';

import { useFieldList } from './FieldListContext';

const Filter = () => {
  const filterFieldInputRef = useRef();
  const { filter, setFilter, resetFilter } = useFieldList();

  return (
    <Form className="form-inline" onSubmit={e => e.preventDefault()}>
      <FilterContainer className="form-group has-feedback">
        <input id="common-search-form-query-input"
               className="query form-control"
               ref={filterFieldInputRef}
               onChange={e => setFilter(e.currentTarget.value)}
               value={filter}
               placeholder="Filter fields"
               type="text"
               autoComplete="off"
               spellCheck="false" />
      </FilterContainer>
      <div className="form-group">
        <Button type="reset" className="reset-button" onClick={resetFilter}>
          Reset
        </Button>
      </div>
    </Form>
  );
};

const Form = styled.form`
  display: flex;
`;

const FilterContainer = styled.div`
  flex-grow: 1;
  margin-right: 5px;

  > input {
    width: 100%;
  }
`;

export default Filter;
