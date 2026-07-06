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
import { useFormikContext } from 'formik';

import type { QueryValidationState, ValidationExplanations } from 'views/components/searchbar/queryvalidation/types';

import QueryValidationDisplay from './QueryValidationDisplay';

export { DocumentationIcon, Explanation } from './QueryValidationDisplay';

type QueryForm = {
  queryString: QueryValidationState;
};

type Props = {
  validationExplanations?: ValidationExplanations;
};

const QueryValidation = ({ validationExplanations = [] }: Props) => {
  const {
    errors: { queryString: queryStringErrors },
  } = useFormikContext<QueryForm>();

  return (
    <QueryValidationDisplay
      queryStringErrors={queryStringErrors as unknown as QueryValidationState}
      validationExplanations={validationExplanations}
    />
  );
};

export default QueryValidation;
