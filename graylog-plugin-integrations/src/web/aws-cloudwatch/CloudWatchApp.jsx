import React from 'react';
import PropTypes from 'prop-types';

import PageHeader from 'components/common/PageHeader';

import { SidebarProvider } from './context/Sidebar';
import { FormDataProvider } from './context/FormData';
import { StepsProvider } from './context/Steps';
import { ApiProvider } from './context/Api';
import CloudWatch from './CloudWatch';

const CloudWatchApp = ({ params: { step }, route }) => {
  return (
    <ApiProvider>
      <StepsProvider>
        <FormDataProvider>
          <SidebarProvider>
            <PageHeader title="AWS Integrations">
              <span>Graylog can integrate and retrieve log messages from various AWS sources.</span>
            </PageHeader>

            <CloudWatch wizardStep={step} route={route} />
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
