// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { Alert } from 'components/graylog';

type Props = {
  children: React.Node,
};

const EmptySearchResult = ({ children }: Props) => (<Alert bsStyle="info">{children}</Alert>);

EmptySearchResult.propTypes = {
  children: PropTypes.element,
};

EmptySearchResult.defaultProps = {
  children: 'No results found for current search!',
};

export default EmptySearchResult;
