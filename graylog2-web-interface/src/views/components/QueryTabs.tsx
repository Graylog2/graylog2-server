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
import { useRef } from 'react';
import type * as Immutable from 'immutable';
import PropTypes from 'prop-types';
import ImmutablePropTypes from 'react-immutable-proptypes';

import { Col, Row } from 'components/bootstrap';
import type { QueryId } from 'views/logic/queries/Query';
import type Query from 'views/logic/queries/Query';
import type { TitlesMap } from 'views/stores/TitleTypes';
import ElementDimensions from 'components/common/ElementDimensions';
import type ViewState from 'views/logic/views/ViewState';

import QueryTitleEditModal from './queries/QueryTitleEditModal';
import AdaptableQueryTabs from './AdaptableQueryTabs';

export interface QueryTabsProps {
  onRemove: (queryId: string) => Promise<void | ViewState>,
  onSelect: (queryId: string) => Promise<Query> | Promise<string>,
  onTitleChange: (queryId: string, newTitle: string) => Promise<TitlesMap>,
  queries: Immutable.OrderedSet<QueryId>,
  activeQueryId: string,
  titles: Immutable.Map<string, string>,
  dashboardId: string,
}

const QueryTabs = ({ onRemove, onSelect, onTitleChange, queries, activeQueryId, titles, dashboardId }: QueryTabsProps) => {
  const queryTitleEditModal = useRef<QueryTitleEditModal | undefined | null>();

  return (
    <Row>
      <Col>
        <ElementDimensions>
          {({ width }) => (width ? (
            <AdaptableQueryTabs maxWidth={width}
                                queries={queries}
                                dashboardId={dashboardId}
                                titles={titles}
                                activeQueryId={activeQueryId}
                                onRemove={onRemove}
                                onSelect={onSelect}
                                queryTitleEditModal={queryTitleEditModal}
                                onTitleChange={onTitleChange} />
          ) : <div />)}
        </ElementDimensions>

        {/*
          The title edit modal can't be part of the QueryTitle component,
          due to the react bootstrap tabs keybindings.
          The input would always lose the focus when using the arrow keys.
        */}
        <QueryTitleEditModal onTitleChange={(newTitle: string) => onTitleChange(activeQueryId, newTitle)}
                             ref={queryTitleEditModal} />
      </Col>
    </Row>
  );
};

QueryTabs.propTypes = {
  onRemove: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
  onTitleChange: PropTypes.func.isRequired,
  queries: ImmutablePropTypes.orderedSetOf(PropTypes.string).isRequired,
  activeQueryId: PropTypes.string.isRequired,
  titles: PropTypes.object.isRequired,
};

export default QueryTabs;
