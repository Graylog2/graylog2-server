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
import PropTypes from 'prop-types';

import { Button, ButtonToolbar } from 'components/bootstrap';

type Props = {
  children: React.ReactNode,
  href: string,
  clusterId: string,
}

const ProductLink = ({ href, clusterId, children }: Props) => {
  let hrefWithParam = href;

  if (clusterId) {
    hrefWithParam = `${hrefWithParam}?cluster_id=${clusterId}`;
  }

  return (
    <ButtonToolbar>
      <Button type="link"
              target="_blank"
              rel="noopener noreferrer"
              href={hrefWithParam}
              bsStyle="primary">
        {children}
      </Button>
    </ButtonToolbar>
  );
};

ProductLink.propTypes = {
  children: PropTypes.node,
  href: PropTypes.string,
  clusterId: PropTypes.string,
};

ProductLink.defaultProps = {
  children: null,
  href: '',
  clusterId: null,
};

export default ProductLink;
