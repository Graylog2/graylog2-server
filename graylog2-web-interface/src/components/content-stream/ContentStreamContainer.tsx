import React from 'react';

import ContentStreamQueryClientProvider from 'contexts/ContentStreamQueryClientProvider';
import ContentStreamSection from 'components/content-stream/ContentStreamSection';

const ContentStreamContainer = () => (
  <ContentStreamQueryClientProvider>
    <ContentStreamSection />
  </ContentStreamQueryClientProvider>
);

export default ContentStreamContainer;
