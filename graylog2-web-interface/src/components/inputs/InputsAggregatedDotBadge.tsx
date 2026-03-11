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

import usePluginEntities from 'hooks/usePluginEntities';

import InputsDotBadge from './InputsDotBadge';

const useInputsBadgeProviderResults = () => {
  const providers = usePluginEntities('inputsBadgeProviders');

  // Safe: providers array is static (registered once at plugin load)
  return providers.map((provider) => provider.useCondition());
};

const InputsAggregatedDotBadge = ({ text }: { text: string }) => {
  const providerResults = useInputsBadgeProviderResults();
  const providerIssue = providerResults.find((r) => r.hasIssues);

  return (
    <InputsDotBadge
      text={text}
      hasExternalIssues={!!providerIssue}
      externalIssuesTitle={providerIssue?.title}
    />
  );
};

export default InputsAggregatedDotBadge;
