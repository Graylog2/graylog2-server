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
import { useState } from 'react';

import { Button } from 'components/bootstrap';
import CreateNewProfileModal from 'components/indices/IndexSetFiledTypeProfiles/CreateNewProfileModal';

const CreateNewProfileButton = () => {
  const [showModal, setShowModal] = useState(false);
  const toggleModal = () => setShowModal((cur) => !cur);

  return (
    <>
      <Button bsStyle="success" onClick={toggleModal}>Create new profile</Button>
      {showModal && <CreateNewProfileModal show={showModal} onClose={toggleModal} />}
    </>
  );
};

export default CreateNewProfileButton;
