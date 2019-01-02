import React from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router';

import Routes from 'routing/Routes';
import connect from 'stores/connect';
import { EntityListItem } from 'components/common';
import StoreProvider from 'injection/StoreProvider';
import UserTimezoneTimestamp from 'enterprise/components/common/UserTimezoneTimestamp';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const formatTitle = (title, id) => <Link to={Routes.pluginRoute('VIEWS_VIEWID')(id)}>{title}</Link>;

// eslint-disable-next-line react/prop-types
const _OwnerTag = ({ owner, currentUser }) => {
  if (!owner || owner === currentUser.username) {
    return <span>Last saved</span>;
  }

  return <span>Shared by {owner}, last saved</span>;
};

const OwnerTag = connect(_OwnerTag, { currentUser: CurrentUserStore }, ({ currentUser }) => ({ currentUser: currentUser.currentUser }));

// eslint-disable-next-line react/prop-types
const Description = ({ description, owner, createdAt }) => (
  <React.Fragment>
    <div>{description || <i>No description given.</i>}</div>
    <div style={{ color: 'darkgray' }}><OwnerTag owner={owner} /> at <UserTimezoneTimestamp dateTime={createdAt} /></div>
  </React.Fragment>
);

const View = ({ children, id, title, summary, description, owner, createdAt }) => (
  <EntityListItem title={formatTitle(title, id)}
                  titleSuffix={summary}
                  description={<Description description={description} owner={owner} createdAt={createdAt} />}
                  actions={children} />
);

View.propTypes = {
  children: PropTypes.node,
  id: PropTypes.string.isRequired,
  title: PropTypes.string,
  summary: PropTypes.string,
  description: PropTypes.string,
  owner: PropTypes.string.isRequired,
  createdAt: PropTypes.string.isRequired,
};

View.defaultProps = {
  children: null,
  title: 'Unnamed View',
  summary: null,
  description: null,
};

export default View;
