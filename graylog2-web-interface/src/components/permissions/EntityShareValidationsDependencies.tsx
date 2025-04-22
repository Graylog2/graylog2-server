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

import type { GranteesList, MissingDependencies } from "logic/permissions/EntityShareState";
import type ValidationResult from "logic/permissions/ValidationResult";

import { ShareFormSection } from "./CommonStyledComponents";
import ValidationError from "./ValidationError";
import DependenciesWarning from "./DependenciesWarning";

type Props = {
  validationResults: ValidationResult,
  missingDependencies: MissingDependencies,
  availableGrantees: GranteesList;
};

const EntityShareValidationsDependencies = ({ validationResults, missingDependencies, availableGrantees }: Props) => (
    <>
      {validationResults?.failed && (
        <ShareFormSection>
          <ValidationError validationResult={validationResults} availableGrantees={availableGrantees} />
        </ShareFormSection>
      )}
      {missingDependencies?.size > 0 && (
        <ShareFormSection>
          <DependenciesWarning missingDependencies={missingDependencies} availableGrantees={availableGrantees} />
        </ShareFormSection>
      )}
    </>
  );

export default EntityShareValidationsDependencies;
