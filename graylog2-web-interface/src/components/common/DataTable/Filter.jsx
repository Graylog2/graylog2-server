import * as React from 'react';
import PropTypes from 'prop-types';

import TypeAheadDataFilter from 'components/common/TypeAheadDataFilter';

const Filter = ({
  children,
  customFilter,
  displayKey,
  filterBy,
  filterKeys,
  filterSuggestions,
  id,
  label,
  onDataFiltered,
  rows,
}) => {
  if (customFilter) {
    return customFilter;
  }

  if (filterKeys.length !== 0) {
    return (
      <div className="row">
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
      </div>
    );
  }

  return null;
};

Filter.propTypes = {
  children: PropTypes.node,
  customFilter: PropTypes.node,
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
  customFilter: undefined,
  displayKey: undefined,
  filterBy: undefined,
  filterKeys: undefined,
  filterSuggestions: undefined,
  label: undefined,
};

export default Filter;
