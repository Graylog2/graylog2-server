import React from 'react';

const InjectActions = (actions) => {
  return {
    contextTypes: {
      actionsProvider: React.PropTypes.object,
    },
    getInitialState: function() {
      this[actions + 'Actions'] = this.context.actionsProvider.getActions(actions)();
    },
  };
};
export default InjectActions;
