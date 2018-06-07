import React from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router';

import Routes from 'routing/Routes';
import { EntityListItem } from 'components/common';

const formatTitle = (title, id) => <Link to={Routes.pluginRoute('VIEWS_VIEWID')(id)}>{title}</Link>;

const View = ({ children, id, title, summary, description }) => (
  <EntityListItem title={formatTitle(title, id)}
                  titleSuffix={summary}
                  description={description || <i>No description given.</i>}
                  actions={children} />
);

View.propTypes = {
  children: PropTypes.node,
  id: PropTypes.string.isRequired,
  title: PropTypes.string,
  summary: PropTypes.string,
  description: PropTypes.string,
};

View.defaultProps = {
  children: null,
  title: 'Unnamed View',
  summary: null,
  description: null,
};

export default View;
