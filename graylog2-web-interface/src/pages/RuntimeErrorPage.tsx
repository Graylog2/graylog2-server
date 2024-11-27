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
import React, { useCallback, useState } from 'react';
import styled from 'styled-components';

import AppConfig from 'util/AppConfig';
import { Icon } from 'components/common';
import { Button } from 'components/bootstrap';
import ErrorPage from 'components/errors/ErrorPage';
import { SupportSources } from 'components/support';
import ClipboardButton from 'components/common/ClipboardButton';

const ToggleDetails = styled.div`
  font-weight: normal;
`;

const description = (
  <>
    <p>It seems like the page you navigated to contained an error.</p>
    <p>You can use the navigation to reach other parts of the product, refresh the page or submit an error report.</p>
  </>
);

type Props = {
  error: Error,
  componentStack: string,
}

const RuntimeErrorPage = ({ error, componentStack }: Props) => {
  const [showDetails, setShowDetails] = useState(AppConfig.gl2DevMode());
  const errorDetails = `\n\nStack Trace:\n\n${error.stack}\n\nComponent Stack:\n${componentStack}`;

  const _toggleDetails = useCallback((e: React.MouseEvent<HTMLButtonElement>) => {
    e.preventDefault();
    setShowDetails((cur) => !cur);
  }, []);

  return (
    <ErrorPage title="Something went wrong." description={description}>
      <div className="content" style={{ padding: '2em' }}>
        <SupportSources />
      </div>
      <dl>
        <dt>
          Error:
          <ToggleDetails className="pull-right">
            <Button bsStyle="link" tabIndex={0} onClick={_toggleDetails}>
              {showDetails ? 'Show less' : 'Show more'}
            </Button>
          </ToggleDetails>
        </dt>
        <dt>
          <pre className="content" id="render-error">
            <div className="pull-right">
              <ClipboardButton title={<Icon name="content_copy" />}
                               bsSize="sm"
                               text={`${error.message}\n${errorDetails}`}
                               buttonTitle="Copy error details to clipboard" />
            </div>
            {error.message}
            {showDetails && errorDetails}
          </pre>
        </dt>
      </dl>
    </ErrorPage>
  );
};

export default RuntimeErrorPage;
