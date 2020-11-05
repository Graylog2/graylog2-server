// @flow strict
import * as React from 'react';

import SectionGrid from 'components/common/Section/SectionGrid';

import HTTPHeaderAuthConfigSection from './HTTPHeaderAuthConfigSection';

const AuthenticatorsDetails = () => (
  <SectionGrid>
    <HTTPHeaderAuthConfigSection />
  </SectionGrid>
);

export default AuthenticatorsDetails;
