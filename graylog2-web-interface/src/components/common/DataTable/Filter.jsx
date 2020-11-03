import * as React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import TypeAheadDataFilter from 'components/common/TypeAheadDataFilter';

const Wrapper = styled.div`
  .control-label {
    padding-top: 0;
  }
`;

const Filter = ({
  children,
  displayKey,
  filterBy,
  filterKeys,
  filterSuggestions,
  id,
  label,
  onDataFiltered,
  rows,
}) => {
  if (filterKeys.length !== 0) {
    return (
      <Wrapper className="row">
        <div className="col-md-8">
          <TypeAheadDataFilter id={`${id}-data-filter`}
                               label={label}
                               data={rows}
                               displayKey={displayKey}
                               filterBy={filterBy}
                               filterSuggestions={filterSuggestions}
                               searchInKeys={filterKeys}
                               onDataFiltered={onDataFiltered} />
        </div>
        {children && (
          <div className="col-md-4">
            {children}
          </div>
        )}
      </Wrapper>
    );
  }

  return null;
};

Filter.propTypes = {
  children: PropTypes.node,
  displayKey: PropTypes.string,
  filterBy: PropTypes.string,
  filterKeys: PropTypes.array,
  filterSuggestions: PropTypes.array,
  id: PropTypes.string.isRequired,
  label: PropTypes.string,
  onDataFiltered: PropTypes.func.isRequired,
  rows: PropTypes.array.isRequired,
};

Filter.defaultProps = {
  children: undefined,
  displayKey: undefined,
  filterBy: undefined,
  filterKeys: undefined,
  filterSuggestions: undefined,
  label: undefined,
};

export default Filter;
