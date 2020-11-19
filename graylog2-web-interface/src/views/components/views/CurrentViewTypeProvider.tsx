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
import * as React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import { ViewStore } from 'views/stores/ViewStore';
import type { ViewType } from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';

type Props = {
  type: ViewType | undefined | null,
  children: React.ReactNode,
};

const CurrentViewTypeProvider = ({ type, children }: Props) => <ViewTypeContext.Provider value={type}>{children}</ViewTypeContext.Provider>;

CurrentViewTypeProvider.propTypes = {
  type: PropTypes.oneOf<ViewType>(['SEARCH', 'DASHBOARD']).isRequired,
  children: PropTypes.node.isRequired,
};

export default connect(
  CurrentViewTypeProvider,
  { view: ViewStore },
  ({ view }) => ({ type: (view && view.view) ? view.view.type : undefined }),
);
