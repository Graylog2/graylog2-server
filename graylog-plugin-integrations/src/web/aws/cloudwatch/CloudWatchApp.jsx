/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import PropTypes from 'prop-types';

import PageHeader from 'components/common/PageHeader';
import withParams from 'routing/withParams';
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

export default withParams(CloudWatchApp);
