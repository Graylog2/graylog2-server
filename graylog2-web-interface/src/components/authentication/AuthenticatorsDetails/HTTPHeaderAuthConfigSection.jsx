// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';

import HTTPHeaderAuthConfigDomain from 'domainActions/authentication/HTTPHeaderAuthConfigDomain';
import { Spinner, ReadOnlyFormGroup } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';

const HTTPHeaderAuthConfigSection = () => {
  const [loadedConfig, setLoadedConfig] = useState();
  const sectionTitle = 'Single Sign-On';

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
      <ReadOnlyFormGroup label="Enabled" value={loadedConfig.enabled} />
      <ReadOnlyFormGroup label="Username header" value={loadedConfig.usernameHeader} />
    </SectionComponent>
  );
};

export default HTTPHeaderAuthConfigSection;
