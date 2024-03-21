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

import { ModalSubmit } from 'components/common';
import { Button } from 'components/bootstrap';
import type { EventDefinitionFormControlsProps } from 'components/event-definitions/event-definitions-types';

const EventDefinitionFormControls = ({
  action,
  activeStepIndex,
  onCancel,
  onOpenNextPage,
  onOpenPrevPage,
  onSubmit,
  steps,
}: EventDefinitionFormControlsProps) => {
  if (activeStepIndex === steps.length - 1) {
    return (
      <ModalSubmit onCancel={onCancel}
                   onSubmit={onSubmit}
                   submitButtonText={`${action === 'edit' ? 'Update' : 'Create'} event definition`} />
    );
  }

  return (
    <div>
      <Button bsStyle="info"
              onClick={onOpenPrevPage}
              disabled={activeStepIndex === 0}>
        Previous
      </Button>
      <div className="pull-right">
        <Button bsStyle="info"
                onClick={onOpenNextPage}>
          Next
        </Button>
      </div>
    </div>
  );
};

export default EventDefinitionFormControls;
