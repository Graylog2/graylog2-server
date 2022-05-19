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
