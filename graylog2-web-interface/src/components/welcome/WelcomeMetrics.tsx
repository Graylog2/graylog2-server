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
import { forwardRef, useMemo, useState } from 'react';
import styled from 'styled-components';

import useHasAccessToAnyStream from 'hooks/useHasAccessToAnyStream';
import useSearchConfiguration from 'hooks/useSearchConfiguration';
import { Alert, Row, Col } from 'components/bootstrap';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import { BLANK } from 'views/components/contexts/SearchPageLayoutContext';
import SearchPageLayoutProvider from 'views/components/contexts/SearchPageLayoutProvider';
import SearchPage from 'views/pages/SearchPage';
import Store from 'logic/local-storage/Store';
import { durationInSeconds } from 'util/DateTime';
import WidgetActionsContext from 'views/components/contexts/WidgetActionsContext';
import { widgetActionsMenuClass } from 'views/components/widgets/Constants';

import useWelcomeMetricsSearch, { DEFAULT_TIME_RANGE_SECONDS } from './hooks/useWelcomeMetricsSearch';
import replayLinkWidgetAction from './ReplayLinkWidgetAction';
const NO_STREAM_ACCESS_DISMISSED_KEY = 'welcome-metrics-no-stream-access-dismissed';
const WIDGET_ACTIONS = [replayLinkWidgetAction];

const Container = styled.div`
  margin-bottom: 6.4px;
  .${widgetActionsMenuClass} {
    opacity: 1;
  }
`;

const StyledAlert = styled(Alert)`
  margin: 0;
`;

const SearchAreaContainer = forwardRef<HTMLDivElement, React.PropsWithChildren>(({ children }, ref) => (
  <div ref={ref}>{children}</div>
));

const MetricsSearchPage = ({ rangeSeconds }: { rangeSeconds: number }) => {
  const view = useWelcomeMetricsSearch(rangeSeconds);

  const searchPageLayoutContextValue = useMemo(
    () => ({
      sidebar: { isShown: false },
      viewActions: BLANK,
      searchAreaContainer: { component: SearchAreaContainer },
    }),
    [],
  );

  return (
    <InteractiveContext.Provider value={false}>
      <WidgetActionsContext.Provider value={WIDGET_ACTIONS}>
        <SearchPageLayoutProvider value={searchPageLayoutContextValue}>
          <SearchPage view={view} isNew={false} skipNoStreamsCheck />
        </SearchPageLayoutProvider>
      </WidgetActionsContext.Provider>
    </InteractiveContext.Provider>
  );
};

const WelcomeMetrics = () => {
  const hasAccessToAnyStream = useHasAccessToAnyStream();
  const { config } = useSearchConfiguration();
  const queryTimeRangeLimitSeconds = durationInSeconds(config?.query_time_range_limit ?? '');
  const [noStreamAccessDismissed, setNoStreamAccessDismissed] = useState(!!Store.get(NO_STREAM_ACCESS_DISMISSED_KEY));

  const onDismissNoStreamAccess = () => {
    Store.set(NO_STREAM_ACCESS_DISMISSED_KEY, true);
    setNoStreamAccessDismissed(true);
  };

  if (!hasAccessToAnyStream) {
    if (noStreamAccessDismissed) {
      return null;
    }

    return (
      <Row className="content">
        <Col xs={12}>
          <StyledAlert onDismiss={onDismissNoStreamAccess}>
            Once you have access to a stream, your message metrics will show up here.
          </StyledAlert>
        </Col>
      </Row>
    );
  }

  const rangeSeconds =
    queryTimeRangeLimitSeconds > 0 && queryTimeRangeLimitSeconds < DEFAULT_TIME_RANGE_SECONDS
      ? queryTimeRangeLimitSeconds
      : DEFAULT_TIME_RANGE_SECONDS;

  return (
    <Container>
      <MetricsSearchPage rangeSeconds={rangeSeconds} />
    </Container>
  );
};

export default WelcomeMetrics;
