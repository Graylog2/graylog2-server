import React from 'react';
import PropTypes from 'prop-types';
import numeral from 'numeral';
import { get } from 'lodash';

import { Timestamp } from 'components/common';
import DateTime from 'logic/datetimes/DateTime';
import CurrentUserStore from 'stores/users/CurrentUserStore';
import connect from 'stores/connect';

const UserTimestamp = connect(Timestamp, { currentUser: CurrentUserStore }, ({ currentUser }) => ({ tz: get(currentUser, 'currentUser.timezone', 'UTC') }));

const SearchResultOverview = ({ results }) => {
  const { timestamp } = results;
  return (
    <span>
      Found <strong>{numeral(results.documentCount).format('0,0')} messages</strong> in {numeral(results.duration).format('0,0')}ms.
      <br />
      Query executed at <UserTimestamp dateTime={timestamp} format={DateTime.Formats.DATETIME} />.
    </span>
  );
};

SearchResultOverview.propTypes = {
  results: PropTypes.object.isRequired,
};

export default SearchResultOverview;
