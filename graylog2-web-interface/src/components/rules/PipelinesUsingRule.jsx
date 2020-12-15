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
import React, { useContext } from 'react';
import PropTypes from 'prop-types';

import { Link } from 'components/graylog/router';
import Routes from 'routing/Routes';
import { PipelineRulesContext } from 'components/rules/RuleContext';
import { Input } from 'components/bootstrap';

import RuleFormStyle from './RuleForm.css';

const PipelinesUsingRule = ({ create }) => {
  const { usedInPipelines } = useContext(PipelineRulesContext);

  if (create) {
    return null;
  }

  const formattedPipelines = () => usedInPipelines.map((pipeline) => {
    return (
      <li key={pipeline.id}>
        <Link to={Routes.SYSTEM.PIPELINES.PIPELINE(pipeline.id)}>
          {pipeline.title}
        </Link>
      </li>
    );
  });

  return (
    <Input id="used-in-pipelines" label="Used in pipelines" help="Pipelines that use this rule in one or more of their stages.">
      <div className="form-control-static">
        {usedInPipelines.length === 0
          ? 'This rule is not being used in any pipelines.' : (
            <ul className={RuleFormStyle.usedInPipelines}>
              {formattedPipelines()}
            </ul>
          )}
      </div>
    </Input>
  );
};

PipelinesUsingRule.propTypes = {
  create: PropTypes.bool.isRequired,
};

export default PipelinesUsingRule;
