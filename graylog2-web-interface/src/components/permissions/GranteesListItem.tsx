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
import { $PropertyType } from 'utility-types';
import { useState } from 'react';
import styled, { StyledComponent } from 'styled-components';
import { Formik, Form } from 'formik';

import { ThemeInterface } from 'theme';
import EntityShareState, { CapabilitiesList } from 'logic/permissions/EntityShareState';
import Grantee from 'logic/permissions/Grantee';
import { Spinner, IconButton } from 'components/common';
import Capability from 'logic/permissions/Capability';
import SelectedGrantee, { CurrentState as CurrentGranteeState } from 'logic/permissions/SelectedGrantee';

import GranteeIcon from './GranteeIcon';
import CapabilitySelect from './CapabilitySelect';

const currentStateColor = (theme: ThemeInterface, currentState: CurrentGranteeState) => {
  switch (currentState) {
    case 'new':
      return theme.colors.variant.lighter.success;
    case 'changed':
      return theme.colors.variant.lighter.warning;
    default:
      return 'transparent';
  }
};

const Container: StyledComponent<{ currentState: CurrentGranteeState }, ThemeInterface, HTMLLIElement> = styled.li(({ theme, currentState }) => `
  display: flex;
  align-items: center;
  width: 100%;
  padding: 5px;
  border-left: 5px solid ${currentStateColor(theme, currentState)};
`);

const GranteeInfo = styled.div`
  display: flex;
  align-items: center;
  flex: 1;
  overflow: hidden;
  margin-right: 10px;
`;

const Title = styled.div`
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const StyledCapabilitySelect = styled(CapabilitySelect)`
  flex: 0.5;
`;

const StyledGranteeIcon = styled(GranteeIcon)`
  margin-right: 5px;
`;

const Actions = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 25px;
  margin-left: 10px;
`;

type Props = {
  availableCapabilities: CapabilitiesList,
  currentGranteeState: CurrentGranteeState,
  grantee: SelectedGrantee,
  onDelete: (grantee: $PropertyType<Grantee, 'id'>) => Promise<EntityShareState | undefined | null>,
  onCapabilityChange: (payload: {
    granteeId: $PropertyType<Grantee, 'id'>,
    capabilityId: $PropertyType<Capability, 'id'>,
  }) => Promise<EntityShareState | undefined | null>,
};

const GranteesListItem = ({ availableCapabilities, currentGranteeState, grantee: { id, capabilityId, type, title }, onDelete, onCapabilityChange }: Props) => {
  const [isDeleting, setIsDeleting] = useState(false);

  const handleDelete = () => {
    setIsDeleting(true);

    onDelete(id).then(() => setIsDeleting(false));
  };

  return (
    <Formik initialValues={{ capabilityId }} onSubmit={() => {}}>
      <Form>
        <Container currentState={currentGranteeState}>
          <GranteeInfo title={title}>
            <StyledGranteeIcon type={type} />
            <Title>{title}</Title>
          </GranteeInfo>
          <StyledCapabilitySelect onChange={(newCapabilityId) => onCapabilityChange({ granteeId: id, capabilityId: newCapabilityId })}
                                  capabilities={availableCapabilities}
                                  title={`Change the capability for ${title}`} />
          <Actions>
            {isDeleting ? (
              <Spinner text="" />
            ) : (
              <IconButton name="trash" onClick={handleDelete} title={`Remove sharing for ${title}`} />
            )}
          </Actions>
        </Container>
      </Form>
    </Formik>
  );
};

export default GranteesListItem;
