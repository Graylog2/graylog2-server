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
import { useContext } from 'react';
import styled, { css } from 'styled-components';

import Spinner from 'components/common/Spinner';
import Query from 'views/components/Query';
import { useStore } from 'stores/connect';
import { SearchLoadingStateStore } from 'views/stores/SearchLoadingStateStore';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import LoadingIndicator from 'components/common/LoadingIndicator';
import { Row, Col } from 'components/bootstrap';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';

const StyledRow = styled(Row)(({ $hasFocusedWidget }: { $hasFocusedWidget: boolean }) => css`
  height: ${$hasFocusedWidget ? '100%' : 'auto'};
  overflow: ${$hasFocusedWidget ? 'auto' : 'visible'};
`);

const StyledCol = styled(Col)`
  height: 100%;
`;

const SearchLoadingIndicator = () => {
  const searchLoadingState = useStore(SearchLoadingStateStore);

  return (searchLoadingState.isLoading && <LoadingIndicator text="Updating search results..." />);
};

type Props = {
  hasErrors: boolean,
};

const SearchResult = React.memo(({ hasErrors }: Props) => {
  const fieldTypes = useContext(FieldTypesContext);
  const { focusedWidget } = useContext(WidgetFocusContext);

  if (!fieldTypes) {
    return <Spinner />;
  }

  const hasFocusedWidget = !!focusedWidget?.id;

  const content = hasErrors ? null : <Query />;

  return (
    <StyledRow $hasFocusedWidget={hasFocusedWidget}>
      <StyledCol>
        {content}
        <SearchLoadingIndicator />
      </StyledCol>
    </StyledRow>
  );
});

SearchResult.displayName = 'SearchResult';

export default SearchResult;
