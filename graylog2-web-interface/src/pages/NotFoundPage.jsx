import React from 'react';

import NotFoundBackgroundImage from 'assets/not-found-bg.jpg';
import { ReportedErrorDetails } from 'components/common';

const NotFoundPage = () => {
  const description = (
    <>
      <p>The party gorilla was just here, but had another party to rock.</p>
      <p>Oh, party gorilla! How we miss you! Will we ever see you again?</p>
    </>
  );
  return (<ReportedErrorDetails title="Page not found" description={description} backgroundImage={NotFoundBackgroundImage} />);
};

export default NotFoundPage;
