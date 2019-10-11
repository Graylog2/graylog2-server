import React, { useRef } from 'react';

import { Button } from 'components/graylog';

import { useFieldList } from './FieldListContext';
import styles from './FieldList.css';

const Filter = () => {
  const filterFieldInputRef = useRef();
  const { filter, setFilter, resetFilter } = useFieldList();

  return (
    <form className={`form-inline ${styles.filterContainer}`} onSubmit={e => e.preventDefault()}>
      <div className={`form-group has-feedback ${styles.filterInputContainer}`}>
        <input id="common-search-form-query-input"
               className="query form-control"
               ref={filterFieldInputRef}
               onChange={e => setFilter(e.currentTarget.value)}
               value={filter}
               placeholder="Filter fields"
               type="text"
               autoComplete="off"
               spellCheck="false" />
      </div>
      <div className="form-group">
        <Button type="reset" className="reset-button" onClick={resetFilter}>
          Reset
        </Button>
      </div>
    </form>
  );
};

export default Filter;
