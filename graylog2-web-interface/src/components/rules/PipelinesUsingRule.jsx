import React from 'react';
import { Link } from 'react-router';

import Routes from 'routing/Routes';

import RuleFormStyle from './RuleForm.css';

const PipelinesUsingRule = () => {
  return (
    <div>
      This rule is not being used in any pipelines.
    </div>
  );
};

export default PipelinesUsingRule;

// get usedInPipelines from RuleContext

// let pipelinesUsingRule;

//     if (!create) {
//       pipelinesUsingRule = (
//         <Input id="used-in-pipelines" label="Used in pipelines" help="Pipelines that use this rule in one or more of their stages.">
//           <div className="form-control-static">
//             {this._formatPipelinesUsingRule()}
//           </div>
//         </Input>
//       );
//     }

//     _formatPipelinesUsingRule = () => {
//       const { usedInPipelines } = this.props;

//       if (usedInPipelines.length === 0) {
//         return 'This rule is not being used in any pipelines.';
//       }

//       const formattedPipelines = usedInPipelines.map((pipeline) => {
//         return (
//           <li key={pipeline.id}>
//             <Link to={Routes.SYSTEM.PIPELINES.PIPELINE(pipeline.id)}>
//               {pipeline.title}
//             </Link>
//           </li>
//         );
//       });

//       return <ul className={RuleFormStyle.usedInPipelines}>{formattedPipelines}</ul>;
//     };
