import React from 'react';
import PropTypes from 'prop-types';

import ErrorJumbotron from 'components/errors/ErrorJumbotron';

/**
 * Component that renders a page when there was an error and certain information can't be fetched. Use it
 * only when the page would make no sense if the information is not available (i.e. a node page where we
 * can't reach the node).
 */
const PageErrorOverview = ({ errors }) => {
  const formattedErrors = errors ? errors.map((error) => <li key={`key-${error.toString()}`}>{error.toString()}</li>) : [];

  return (
    <ErrorJumbotron>
      <H1>Error getting data</H1>
      <p>We had trouble fetching some data required to build this page, so here is a picture instead.</p>
      <ul>
        {formattedErrors}
        <li>Check your Graylog logs for more information.</li>
      </ul>
    </ErrorJumbotron>
  );
};

PageErrorOverview.propTypes = {
  /** Array of errors that prevented the original page to load. */
  errors: PropTypes.array.isRequired,
};

export default PageErrorOverview;
