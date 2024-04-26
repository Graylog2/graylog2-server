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

import { Button, ButtonToolbar, MenuItem } from 'components/bootstrap';
import Routes from 'routing/Routes';
import { LinkContainer } from 'components/common/router';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';
import MoreActions from 'components/common/EntityDataTable/MoreActions';
import useTemplateMutation from 'components/indices/IndexSetTemplates/hooks/useTemplateMutation';

const TemplateActions = ({ templateId, templateTitle }: { templateId: string, templateTitle: string }) => {
  const { deselectEntity } = useSelectedEntities();
  const { deleteTemplate } = useTemplateMutation();

  const onDelete = () => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`You are about to delete template: "${templateTitle}". Are you sure?`)) {
      deleteTemplate(templateId).then(() => {
        deselectEntity(templateId);
      });
    }
  };

  return (
    <ButtonToolbar>
      <LinkContainer to={Routes.SYSTEM.INDICES.TEMPLATES.edit(templateId)}>
        <Button bsSize="xs">
          Edit
        </Button>
      </LinkContainer>
      <MoreActions>
        <MenuItem onSelect={onDelete}>
          Delete
        </MenuItem>
      </MoreActions>
    </ButtonToolbar>
  );
};

export default TemplateActions;
