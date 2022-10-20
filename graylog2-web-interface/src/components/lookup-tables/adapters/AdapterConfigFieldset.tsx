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
import * as React from 'react';

import type { LookupTableDataAdapterConfig } from 'logic/lookup-tables/types';

type ConfigFieldsetAttributes = {
  config?: LookupTableDataAdapterConfig,
  handleFormEvent?: (arg: React.BaseSyntheticEvent) => void,
  updateConfig?: (newConfig: LookupTableDataAdapterConfig) => void,
  validationMessage?: (fieldName: string, defaultText: string) => JSX.Element,
  validationState?: (fieldName: string) => string,
  setDisableFormSubmission?: boolean,
};

type ConfigFieldsetProps = ConfigFieldsetAttributes & {
  formComponent: React.FC,
};

const AdapterConfigFieldset = ({
  formComponent,
  config,
  handleFormEvent,
  updateConfig,
  validationMessage,
  validationState,
  setDisableFormSubmission,
}: ConfigFieldsetProps) => {
  return React.createElement<ConfigFieldsetAttributes>(
    formComponent, {
      config,
      handleFormEvent,
      updateConfig,
      validationMessage,
      validationState,
      setDisableFormSubmission,
    },
  );
};

AdapterConfigFieldset.defaultProps = {
  config: { type: 'none' },
  handleFormEvent: () => {},
  updateConfig: () => {},
  validationMessage: {},
  validationState: null,
  setDisableFormSubmission: false,
};

export default AdapterConfigFieldset;
