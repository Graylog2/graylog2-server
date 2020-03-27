// @flow strict
import React, { useContext } from 'react';

import View from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';

import { Alert, Row, Col } from 'components/graylog';
import { Icon } from 'components/common';
import AppContentGrid from 'components/layout/AppContentGrid';

type CommonErrorType = {
  error: {
    additional: {
      body: { message: string, streams: string[]}
    }
  }
}

const CommonError = ({ error: { additional: { body: { message, streams } } } }: CommonErrorType) => (
  <>
    <p><b><Icon name="info-circle" /> {message}</b></p>
    {streams && (
      <p>
        Please get in contact with the dashboard owner and provide the ids of streams you need access for:<br />
        {streams.join(', ')}<br />
      </p>
    )}
  </>
);

const SearchExecutionErrors = ({ errors }: { errors: Object[] }) => {
  const viewType = useContext(ViewTypeContext);
  const viewTypeName = viewType === View.Type.Dashboard ? 'dashboard' : 'search';
  return (
    <AppContentGrid>
      <Row className="content">
        <Col md={12}>
          <h2>The {viewTypeName} can&apos;t be accessed</h2>
          <p>This is mostly related to missing access rights. Please have a look at the following errors.</p>
          {errors.map((error, index) => {
            const { additional } = error;
            return (
              // eslint-disable-next-line react/no-array-index-key
              <Alert bsStyle="danger" role="alert" key={index}>
                {(additional && additional.body) ? <CommonError error={error} /> : JSON.stringify(error)}
              </Alert>
            );
          })}
        </Col>
      </Row>
    </AppContentGrid>
  );
};

export default SearchExecutionErrors;
