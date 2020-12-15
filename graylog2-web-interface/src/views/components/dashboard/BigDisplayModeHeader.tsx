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

import connect from 'stores/connect';
import Spinner from 'components/common/Spinner';
import { ViewStore } from 'views/stores/ViewStore';
import queryTitle from 'views/logic/queries/QueryTitle';
import type { QueryId } from 'views/logic/queries/Query';
import View from 'views/logic/views/View';

type Props = {
  view: {
    activeQuery?: QueryId,
    view?: View,
  },
};

const PositioningWrapper = styled.div`
  padding-left: 20px;
`;

const BigDisplayModeHeader = ({ view: { activeQuery, view } = { activeQuery: undefined, view: undefined } }: Props) => {
  if (!view || !activeQuery) {
    return <Spinner />;
  }

  const currentQueryTitle = queryTitle(view, activeQuery);

  return (
    <PositioningWrapper>
      <h1>{view.title}</h1>
      <h2>{currentQueryTitle}</h2>
    </PositioningWrapper>
  );
};

BigDisplayModeHeader.propTypes = {
  view: PropTypes.shape({
    view: PropTypes.instanceOf(View),
    activeQuery: PropTypes.string,
  }),
};

BigDisplayModeHeader.defaultProps = {
  view: undefined,
};

export default connect(BigDisplayModeHeader, { view: ViewStore });
