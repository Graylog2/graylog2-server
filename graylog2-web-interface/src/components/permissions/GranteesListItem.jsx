// @flow strict
import * as React from 'react';
import { useState } from 'react';
import styled, { type StyledComponent } from 'styled-components';
import { Formik, Form } from 'formik';

import { type ThemeInterface } from 'theme';
import EntityShareState, { type CapabilitiesList } from 'logic/permissions/EntityShareState';
import Grantee from 'logic/permissions/Grantee';
import { Spinner, IconButton } from 'components/common';
import Capability from 'logic/permissions/Capability';
import SelectedGrantee, { type CurrentState as CurrentGranteeState } from 'logic/permissions/SelectedGrantee';

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
  onDelete: ($PropertyType<Grantee, 'id'>) => Promise<?EntityShareState>,
  onCapabilityChange: ({
    granteeId: $PropertyType<Grantee, 'id'>,
    capabilityId: $PropertyType<Capability, 'id'>,
  }) => Promise<?EntityShareState>,
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
          <GranteeInfo>
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
