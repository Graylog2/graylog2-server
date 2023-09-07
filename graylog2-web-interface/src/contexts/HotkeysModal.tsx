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
import React, { useCallback, useState } from 'react';
import PropTypes from 'prop-types';

import { Modal } from 'components/bootstrap';
import ModalSubmit from 'components/common/ModalSubmit';
import StringUtils from 'util/StringUtils';
import useHotkeysContext from 'hooks/useHotkeysContext';
import useHotkeys from 'hooks/useHotkeys';

type Props = {
    show: boolean,
    onHide: () => void,
};

/**
 * Component that displays a confirmation dialog box that the user can
 * cancel or confirm.
 */
const HotkeysModal = () => {
  const [show, setShow] = useState(false);

  const onHide = useCallback(() => setShow(false), []);
  const onToggle = useCallback(() => setShow((cur) => !cur), []);
  const { hotKeysCollection, activeHotkeys } = useHotkeysContext();
  useHotkeys('SHOW_HELPER', onToggle, { scopes: 'view' });

  return show && (
    <Modal show={show}
           onHide={onHide}>
      <Modal.Header closeButton>
        <Modal.Title>Hotkeys</Modal.Title>
      </Modal.Header>

      <Modal.Body>
        {
          Object.entries(hotKeysCollection).map(([scope, { title, actions, description }]) => (
            <div>
              <b>{title}</b><br />
              <i>{description}</i><br />
              {
                  Object.entries(actions).filter(([actionKey]) => activeHotkeys.has(`${scope}.${actionKey}`)).map(([_, { description: keyDescription, keys }]) => <div><b>{keyDescription}</b><i>{keys}</i></div>)
                }
            </div>
          ))
          }
      </Modal.Body>
    </Modal>
  );
};

export default HotkeysModal;
