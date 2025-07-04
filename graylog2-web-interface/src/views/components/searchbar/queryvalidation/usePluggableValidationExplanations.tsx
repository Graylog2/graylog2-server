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
import React, { useRef } from 'react';

import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';
import usePluginEntities from 'hooks/usePluginEntities';
import type { ValidationErrorExplanation } from 'views/types';

const usePluggableValidationExplanations = (validationState: QueryValidationState) => {
  const modalRefs = useRef({});
  const pluggableValidationExplanations = usePluginEntities('views.elements.validationErrorExplanation');
  const availableExplanations: Array<ValidationErrorExplanation> = pluggableValidationExplanations.filter(
    (validationError) => {
      if (typeof validationError?.useCondition === 'function') {
        return validationError.useCondition(validationState);
      }

      return true;
    },
  );

  const explanationComponents = availableExplanations.map(({ component: PluggableExplanation, key }) => (
    <PluggableExplanation key={`explanation-component-${key}`}
                          validationState={validationState}
                          modalRef={() => modalRefs.current[key]} />
  ));

  const explanationModals = availableExplanations
    .filter(({ modal }) => !!modal)
    .map(({ modal: PluggableExplanationModal, key }) => (
      <PluggableExplanationModal key={`explanation-modal-${key}`}
                                 ref={(r) => { modalRefs.current[key] = r; }}
                                 validationState={validationState} />
    ));

  return ({ explanationComponents, explanationModals });
};

export default usePluggableValidationExplanations;
