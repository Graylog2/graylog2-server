// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
// $FlowFixMe: imports from core need to be fixed in flow
import { Link } from 'react-router';

// $FlowFixMe: imports from core need to be fixed in flow
import Routes from 'routing/Routes';
// $FlowFixMe: imports from core need to be fixed in flow
import connect from 'stores/connect';
// $FlowFixMe: imports from core need to be fixed in flow
import { EntityListItem } from 'components/common';
// $FlowFixMe: imports from core need to be fixed in flow
import StoreProvider from 'injection/StoreProvider';
import UserTimezoneTimestamp from 'views/components/common/UserTimezoneTimestamp';
import withPluginEntities from 'views/logic/withPluginEntities';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const formatTitle = (title, id, disabled = false) => (disabled
  ? <h2>{title}</h2>
  : <Link to={Routes.pluginRoute('VIEWS_VIEWID')(id)}>{title}</Link>);

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

const missingRequirements = (requires, requirementsProvided) => Object.entries(requires)
  .filter(([require]) => !requirementsProvided.includes(require))
  .reduce((prev, [key, value]) => ({ ...prev, [key]: value }), {});
const isMissingRequirements = (requires, requirementsProvided) => Object.keys(missingRequirements(requires, requirementsProvided)).length > 0;

type RequirementsProps = {
  requires: Array<string>,
  requirementsProvided: Array<string>,
};

const Requirements = ({ requires, requirementsProvided }: RequirementsProps) => {
  const missing = missingRequirements(requires, requirementsProvided);
  return Object.keys(missing).length > 0 ? <h5>Missing requirement(s): <strong>{Object.keys(missing).join(', ')}</strong></h5> : null;
};

const View = ({ children, id, title, summary, description, owner, requires, createdAt, requirementsProvided }) => (
  <EntityListItem title={formatTitle(title, id, isMissingRequirements(requires, requirementsProvided))}
                  titleSuffix={summary}
                  description={(
                    <React.Fragment>
                      <Description description={description} owner={owner} createdAt={createdAt} />
                      <Requirements requires={requires} requirementsProvided={requirementsProvided} />
                    </React.Fragment>
                  )}
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
  requires: PropTypes.arrayOf(PropTypes.string).isRequired,
  requirementsProvided: PropTypes.arrayOf(PropTypes.string).isRequired,
};

View.defaultProps = {
  children: null,
  title: 'Unnamed View',
  summary: null,
  description: null,
};

export default withPluginEntities(View, { requirementsProvided: 'views.requires.provided' });
