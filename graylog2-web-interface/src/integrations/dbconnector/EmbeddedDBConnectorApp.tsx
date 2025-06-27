import React from "react";

import { toGenericInputCreateRequest } from './formDataAdapter';
import DBConnector from './DBConnector';
import INITIAL_FORMDATA from './_initialFormData';

import { AdvancedOptionsProvider } from '../common/context/AdvancedOptions';
import { SidebarProvider } from '../common/context/Sidebar';
import { StepsProvider } from '../common/context/Steps';
import { FormDataProvider } from '../common/context/FormData';

type Props = {
  onSubmit?: (InputCreateRequest) => void;
};

const EmbeddedDBConnectorApp = ({ onSubmit = undefined }: Props) => {
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
            <DBConnector onSubmit={handleSubmit} externalInputSubmit={typeof onSubmit === 'function'} />
          </AdvancedOptionsProvider>
        </SidebarProvider>
      </FormDataProvider>
    </StepsProvider>
  );
};

export default EmbeddedDBConnectorApp;