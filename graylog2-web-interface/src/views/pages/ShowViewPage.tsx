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

import useParams from 'routing/useParams';
import useViewLoader from 'views/logic/views/UseViewLoader';
import Spinner from 'components/common/Spinner';
import type { ViewLoaderFn } from 'views/logic/views/ViewLoader';
import ViewLoader from 'views/logic/views/ViewLoader';
import useQuery from 'routing/useQuery';

import SearchPage from './SearchPage';

type Props = {
  viewLoader?: ViewLoaderFn,
};

const ShowViewPage = ({ viewLoader }: Props) => {
  const query = useQuery();
  const { viewId } = useParams<{ viewId?: string }>();

  if (!viewId) {
    throw new Error('No view id specified!');
  }

  const [loaded, HookComponent] = useViewLoader(viewId, query, viewLoader);

  if (HookComponent) {
    return HookComponent;
  }

  if (!loaded) {
    return <Spinner />;
  }

  return <SearchPage />;
};

ShowViewPage.propTypes = {
  viewLoader: PropTypes.func,
};

ShowViewPage.defaultProps = {
  viewLoader: ViewLoader,
};

export default ShowViewPage;
