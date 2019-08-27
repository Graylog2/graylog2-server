// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
// $FlowFixMe: imports from core need to be fixed in flow
import { Link } from 'react-router';

import Routes from 'routing/Routes';
import connect from 'stores/connect';
import { EntityListItem } from 'components/common';
import StoreProvider from 'injection/StoreProvider';
import UserTimezoneTimestamp from 'views/components/common/UserTimezoneTimestamp';
import withPluginEntities from 'views/logic/withPluginEntities';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const formatTitle = (title, id, disabled = false) => (disabled
  ? <h2>{title}</h2>
  : <Link to={Routes.pluginRoute('DASHBOARDS_VIEWID')(id)}>{title}</Link>);

const _OwnerTag = ({ owner, currentUser }) => {
  if (!owner || owner === currentUser.username) {
    return <span>Last saved</span>;
  }

  return <span>Shared by {owner}, last saved</span>;
};

_OwnerTag.propTypes = {
  owner: PropTypes.string.isRequired,
  currentUser: PropTypes.shape({
    username: PropTypes.string.isRequired,
  }).isRequired,
};

const OwnerTag = connect(_OwnerTag, { currentUser: CurrentUserStore }, ({ currentUser }) => ({ currentUser: currentUser.currentUser }));

// eslint-disable-next-line react/prop-types
const Description = ({ description, owner, createdAt }) => (
  <React.Fragment>
    <div>{description || <i>No description given.</i>}</div>
    <div style={{ color: 'darkgray' }}><OwnerTag owner={owner} /> at <UserTimezoneTimestamp dateTime={createdAt} /></div>
  </React.Fragment>
);

type Plugin = {
  name: string,
  url: string,
};

type RequirementsList = { [string]: Plugin };

const missingRequirements = (requires, requirementsProvided): RequirementsList => Object.entries(requires)
  .filter(([require]) => !requirementsProvided.includes(require))
  .reduce((prev, [key, value]) => ({ ...prev, [key]: value }), {});

const isMissingRequirements = (requires, requirementsProvided) => Object.keys(missingRequirements(requires, requirementsProvided)).length > 0;

type RequirementsProps = {
  requires: RequirementsList,
  requirementsProvided: Array<string>,
};

type MissingRequirementProps = {
  plugin: Plugin,
};

const MissingRequirement = ({ plugin }: MissingRequirementProps) => (
  <a href={plugin.url} target="_blank" rel="noopener noreferrer"><strong>{plugin.name}</strong></a>
);

const Requirements = ({ requires, requirementsProvided }: RequirementsProps) => {
  const missing = missingRequirements(requires, requirementsProvided);
  return Object.keys(missing).length > 0
    ? (
      <h5>
        Missing requirement(s): {Object.values(missing)
        // $FlowFixMe: plugin is of type Plugin, not mixed.
        .map((plugin: Plugin) => <MissingRequirement key={plugin.name} plugin={plugin} />)}
      </h5>
    )
    : null;
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
  requires: PropTypes.objectOf(PropTypes.shape({
    name: PropTypes.string.isRequired,
    url: PropTypes.string.isRequired,
  })).isRequired,
  requirementsProvided: PropTypes.arrayOf(PropTypes.string).isRequired,
};

View.defaultProps = {
  children: null,
  title: 'Unnamed View',
  summary: null,
  description: null,
};

export default withPluginEntities(View, { requirementsProvided: 'views.requires.provided' });
