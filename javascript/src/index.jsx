import React from 'react';
import ReactDOM from 'react-dom';
import AppFacade from 'routing/AppFacade';

window.onload = () => {
  const appContainer = document.createElement('div');
  document.body.appendChild(appContainer)
  ReactDOM.render(<AppFacade />, appContainer);
};
