import React from 'react';
import ReactDOM from 'react-dom';
import AppFacade from 'routing/AppFacade';

window.onload = () => {
  ReactDOM.render(<AppFacade />, document.getElementById('app-container'));
};
