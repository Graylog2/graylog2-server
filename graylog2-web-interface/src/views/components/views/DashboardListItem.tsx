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
import React from 'react';
import PropTypes from 'prop-types';

import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import { EntityListItem } from 'components/common';
import useCurrentUser from 'hooks/useCurrentUser';
import Timestamp from 'components/common/Timestamp';
import withPluginEntities from 'views/logic/withPluginEntities';

const formatTitle = (title, id, disabled = false) => (disabled
  ? <h2>{title}</h2>
  : <Link to={Routes.pluginRoute('DASHBOARDS_VIEWID')(id)}>{title}</Link>);

const OwnerTag = ({ owner }) => {
  const currentUser = useCurrentUser();

  if (!owner || owner === currentUser?.username) {
    return <span>Last saved</span>;
  }

  return <span>Shared by {owner}, last saved</span>;
};

OwnerTag.propTypes = {
  owner: PropTypes.string.isRequired,
};

// eslint-disable-next-line react/prop-types
const Description = ({ description, owner, createdAt }) => (
  <>
    <div>{description || <i>No description given.</i>}</div>
    <div style={{ color: 'darkgray' }}><OwnerTag owner={owner} /> at <Timestamp dateTime={createdAt} /></div>
  </>
);

type Plugin = {
  name: string,
  url: string,
};

type RequirementsList = { [key: string]: Plugin };

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

const Requirements = ({ requires, requirementsProvided }: RequirementsProps) => {
  const missing = missingRequirements(requires, requirementsProvided);

  return Object.keys(missing).length > 0
    ? (
      <h5>
        Missing requirement(s): {Object.values(missing)
        .map((plugin: Plugin) => <MissingRequirement key={plugin.name} plugin={plugin} />)}
      </h5>
    )
    : null;
};

const DashboardListItem = ({ children, id, title, summary, description, owner, requires, createdAt, requirementsProvided }) => (
  <EntityListItem title={formatTitle(title, id, isMissingRequirements(requires, requirementsProvided))}
                  titleSuffix={summary}
                  description={(
                    <>
                      <Description description={description} owner={owner} createdAt={createdAt} />
                      <Requirements requires={requires} requirementsProvided={requirementsProvided} />
                    </>
                  )}
                  actions={children} />
);

DashboardListItem.propTypes = {
  children: PropTypes.node,
  id: PropTypes.string.isRequired,
  title: PropTypes.string,
  summary: PropTypes.string,
  description: PropTypes.string,
  owner: PropTypes.string.isRequired,
  createdAt: PropTypes.instanceOf(Date).isRequired,
  requires: PropTypes.objectOf(PropTypes.shape({
    name: PropTypes.string.isRequired,
    url: PropTypes.string.isRequired,
  })).isRequired,
  requirementsProvided: PropTypes.arrayOf(PropTypes.string).isRequired,
};

DashboardListItem.defaultProps = {
  children: null,
  title: 'Unnamed Dashboard',
  summary: null,
  description: null,
};

export default withPluginEntities(DashboardListItem, { requirementsProvided: 'views.requires.provided' });
