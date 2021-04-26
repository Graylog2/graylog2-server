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
import * as Immutable from 'immutable';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { SizeMe } from 'react-sizeme';

import { Col, Row } from 'components/graylog';
import QueryTitleEditModal from 'views/components/queries/QueryTitleEditModal';
import Query, { QueryId } from 'views/logic/queries/Query';
import type { TitlesMap } from 'views/stores/TitleTypes';
import ViewState from 'views/logic/views/ViewState';
import AdaptableQueryTabs from 'views/AdaptableQueryTabs';

type Props = {
  onRemove: (queryId: string) => Promise<void> | Promise<ViewState>,
  onSelect: (queryId: string) => Promise<Query> | Promise<string>,
  onTitleChange: (queryId: string, newTitle: string) => Promise<TitlesMap>,
  queries: Array<QueryId>,
  selectedQueryId: string,
  titles: Immutable.Map<string, string>,
};

const StyledRow = styled(Row)`
  margin-bottom: 0;
`;

const QueryTabs = ({ onRemove, onSelect, onTitleChange, queries, selectedQueryId, titles }:Props) => {
  const queryTitleEditModal = useRef<QueryTitleEditModal | undefined | null>();

  return (
    <StyledRow>
      <Col>
        <SizeMe>
          {({ size }) => {
            return size.width && (
            <AdaptableQueryTabs maxWidth={size.width}
                                queries={queries}
                                titles={titles}
                                selectedQueryId={selectedQueryId}
                                onRemove={onRemove}
                                onSelect={onSelect}
                                queryTitleEditModal={queryTitleEditModal} />
            );
          }}
        </SizeMe>

        {/*
          The title edit modal can't be part of the QueryTitle component,
          due to the react bootstrap tabs keybindings.
          The input would always lose the focus when using the arrow keys.
        */}
        <QueryTitleEditModal onTitleChange={(newTitle: string) => onTitleChange(selectedQueryId, newTitle)}
                             ref={queryTitleEditModal} />
      </Col>
    </StyledRow>
  );
};

QueryTabs.propTypes = {
  onRemove: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
  onTitleChange: PropTypes.func.isRequired,
  queries: PropTypes.object.isRequired,
  selectedQueryId: PropTypes.string.isRequired,
  titles: PropTypes.object.isRequired,
};

export default QueryTabs;
