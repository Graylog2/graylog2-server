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
import PropTypes from 'prop-types';
import React from 'react';

import Panel from './Panel';

type Props = {
  children: React.ReactNode,
  name: string,
  id?: string,
}

const Accordion = ({ children, name, id, ...restProps }: Props) => {
  if (!name) {
    return <></>;
  }

  const eventKey = id ?? name.replace(/[^0-9a-zA-Z]/g, '-').toLowerCase();

  return (
    <Panel {...restProps} id={id} eventKey={eventKey} >
      <Panel.Heading>
        <Panel.Title toggle>
          {name}
        </Panel.Title>
      </Panel.Heading>
      <Panel.Collapse>
        <Panel.Body>
          {children}
        </Panel.Body>
      </Panel.Collapse>
    </Panel>
  );
};

Accordion.propTypes = {
  name: PropTypes.string.isRequired,
  id: PropTypes.string,
  children: PropTypes.node.isRequired,
};

Accordion.defaultProps = {
  id: undefined,
};

export default Accordion;
