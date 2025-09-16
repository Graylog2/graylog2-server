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
import FormDataProvider from 'integrations/contexts/FormDataProvider';

import { toGenericInputCreateRequest } from './common/formDataAdapter';

import { SidebarProvider } from '../context/Sidebar';
import { StepsProvider } from '../context/Steps';
import { AdvancedOptionsProvider } from '../context/AdvancedOptions';
import CloudTrail from './CloudTrail';
import INITIAL_FORMDATA from './_initialFormData';

type Props = {
  onSubmit?: (InputCreateRequest) => void;
};

const EmbeddedCloudTrailApp = ({ onSubmit = undefined }: Props) => {
  const handleSubmit = (formData) => {
    if (!onSubmit) {
      return;
    }

    onSubmit(toGenericInputCreateRequest(formData));
  };

  return (
    <StepsProvider>
      <FormDataProvider initialFormData={INITIAL_FORMDATA}>
        <SidebarProvider>
          <AdvancedOptionsProvider>
            <CloudTrail onSubmit={handleSubmit} externalInputSubmit={typeof onSubmit === 'function'} />
          </AdvancedOptionsProvider>
        </SidebarProvider>
      </FormDataProvider>
    </StepsProvider>
  );
};

export default EmbeddedCloudTrailApp;

