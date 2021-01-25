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
