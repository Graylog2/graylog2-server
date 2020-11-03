// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';

import HTTPHeaderAuthConfigDomain from 'domainActions/authentication/HTTPHeaderAuthConfigDomain';
import { Spinner, ReadOnlyFormGroup } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';

const HTTPHeaderAuthConfigSection = () => {
  const [loadedConfig, setLoadedConfig] = useState();
  const sectionTitle = 'Trusted Header Authentication';

  useEffect(() => {
    HTTPHeaderAuthConfigDomain.load().then(setLoadedConfig);
  }, []);

  if (!loadedConfig) {
    return (
      <SectionComponent title={sectionTitle}>
        <Spinner />
      </SectionComponent>
    );
  }

  return (
    <SectionComponent title={sectionTitle}>
      <p>This authenticator enables you to login a user, based on a HTTP header without further interaction.</p>
      <ReadOnlyFormGroup label="Enabled" value={loadedConfig.enabled} />
      <ReadOnlyFormGroup label="Username header" value={loadedConfig.usernameHeader} />
    </SectionComponent>
  );
};

export default HTTPHeaderAuthConfigSection;
