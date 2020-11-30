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
// @flow strict
import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import numeral from 'numeral';

import AppConfig from 'util/AppConfig';
import { Timestamp } from 'components/common';
import CurrentUserContext from 'contexts/CurrentUserContext';
import DateTime from 'logic/datetimes/DateTime';

type Props = {
  results: {
    timestamp?: string,
    duration?: number,
  },
};

const SearchResultOverview = ({ results: { timestamp, duration } }: Props) => {
  const currentUser = useContext(CurrentUserContext);
  const timezone = currentUser?.timezone ?? AppConfig.rootTimeZone();

  if (!timestamp || !duration) {
    return <i>No query executed yet.</i>;
  }

  return (
    <span>
      Query executed in {numeral(duration).format('0,0')}ms at <Timestamp dateTime={timestamp} format={DateTime.Formats.DATETIME} tz={timezone} />.
    </span>
  );
};

SearchResultOverview.propTypes = {
  results: PropTypes.object.isRequired,
};

export default SearchResultOverview;
