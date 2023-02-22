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
import styled from 'styled-components';

import Spinner from 'components/common/Spinner';
import queryTitle from 'views/logic/queries/QueryTitle';
import useView from 'views/hooks/useView';
import useActiveQueryId from 'views/hooks/useActiveQueryId';

const PositioningWrapper = styled.div`
  padding-left: 20px;
`;

const BigDisplayModeHeader = () => {
  const view = useView();
  const activeQuery = useActiveQueryId();

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

export default BigDisplayModeHeader;
