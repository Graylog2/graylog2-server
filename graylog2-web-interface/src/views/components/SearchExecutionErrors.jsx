// @flow strict
import React, { useContext } from 'react';
import PropTypes from 'prop-types';

import { FetchError } from 'logic/rest/FetchProvider';
import View from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';

import { Alert, Row, Col } from 'components/graylog';
import { Icon } from 'components/common';
import AppContentGrid from 'components/layout/AppContentGrid';

const FetchErrorMessage = ({ error: { additional: { body: { message, streams, type } } } }: { error: FetchError }) => (
  <Alert bsStyle="danger" role="alert">
    <p>
      <b><Icon name="info-circle" /> {message}</b>
    </p>
    {type === 'MissingStreamPermission' && (
      <p>
        Please get in contact with a graylog administrator.
        {streams && streams.length > 0 && (
          <>
            {' '}You need permission to streams with the id:<br />
            {streams.join(', ')}
          </>
        )}
      </p>
    )}
  </Alert>
);

const SearchExecutionErrors = ({ errors }: { errors: FetchError[] }) => {
  const viewType = useContext(ViewTypeContext);
  const viewTypeName = viewType === View.Type.Dashboard ? 'dashboard' : 'search';
  return (
    <AppContentGrid>
      <Row className="content">
        <Col md={12}>
          <h2>The {viewTypeName} can&apos;t be accessed</h2>
          <p>This is mostly related to missing permissions. Please have a look at the following errors.</p>
          {errors.map((error, index) => {
            if (error && error.additional && error.additional.body) {
              // eslint-disable-next-line react/no-array-index-key
              return <FetchErrorMessage error={error} key={index} />;
            }
            // eslint-disable-next-line react/no-array-index-key
            return <pre key={index}>{JSON.stringify(error)}</pre>;
          })}
        </Col>
      </Row>
    </AppContentGrid>
  );
};

SearchExecutionErrors.propTypes = {
  errors: PropTypes.arrayOf(PropTypes.object).isRequired,
};

export default SearchExecutionErrors;
