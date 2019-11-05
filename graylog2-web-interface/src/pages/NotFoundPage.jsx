import React from 'react';
import { DocumentTitle } from 'components/common';

import ErrorJumbotron, { H1 } from './ErrorJumbotron';

const NotFoundPage = () => {
  return (
    <DocumentTitle title="Not Found">
      <ErrorJumbotron>
        <H1>Page not found</H1>
        <p>The party gorilla was just here, but had another party to rock.</p>
        <p>Oh, party gorilla! How we miss you! Will we ever see you again?</p>
      </ErrorJumbotron>
    </DocumentTitle>
  );
};

export default NotFoundPage;
