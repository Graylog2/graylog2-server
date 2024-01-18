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
import React, { useState } from 'react';
import { styled } from 'styled-components';

import useIndexProfileWithMappingsByField
  from 'components/indices/IndexSetFieldTypes/hooks/useIndexProfileWithMappingsByField';
import { IconButton } from 'components/common';
import SetProfileModal from 'components/indices/IndexSetFieldTypes/SetProfileModal';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

const Container = styled.div`
  display: flex;
  gap: 5px;
  align-items: center;
`;

const IndexSetProfile = () => {
  const { name, id, description } = useIndexProfileWithMappingsByField();
  const [showSetModal, setShowSetModal] = useState(false);
  const toggleModal = () => setShowSetModal((cur) => !cur);
  const title = id ? description : 'Field type mapping profile not set yet';

  return (
    <Container title={title}>
      <b>Field type mapping profile:</b>
      {id ? <Link target="_blank" to={Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.edit(id)}>{name}</Link> : <i>Not set</i>}
      <IconButton name="edit" onClick={toggleModal} title="Set field type profile" />
      {showSetModal && <SetProfileModal show={showSetModal} onClose={toggleModal} currentProfile={id} />}
    </Container>
  );
};

export default IndexSetProfile;
