import React from 'react';
import PropTypes from 'prop-types';

import PageHeader from 'components/common/PageHeader';

import { SidebarProvider } from 'aws/context/Sidebar';
import { FormDataProvider } from 'aws/context/FormData';
import { StepsProvider } from 'aws/context/Steps';
import { ApiProvider } from 'aws/context/Api';
import { AdvancedOptionsProvider } from 'aws/context/AdvancedOptions';

import CloudWatch from './CloudWatch';
import INITIAL_FORMDATA from './_initialFormData';

const CloudWatchApp = ({ params: { step }, route }) => {
  return (
    <ApiProvider>
      <StepsProvider>
        <FormDataProvider initialFormData={INITIAL_FORMDATA}>
          <SidebarProvider>
            <AdvancedOptionsProvider>
              <PageHeader title="AWS Integrations">
                <span>This feature retrieves log messages from various AWS sources.</span>
              </PageHeader>

              <CloudWatch wizardStep={step} route={route} />
            </AdvancedOptionsProvider>
          </SidebarProvider>
        </FormDataProvider>
      </StepsProvider>
    </ApiProvider>
  );
};

CloudWatchApp.propTypes = {
  params: PropTypes.shape({
    step: PropTypes.string,
  }).isRequired,
  route: PropTypes.object.isRequired,
};

export default CloudWatchApp;
