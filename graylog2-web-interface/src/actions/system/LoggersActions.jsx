import Reflux from 'reflux';

const LoggersActions = Reflux.createActions({
  loggers: { asyncResult: true },
  subsystems: { asyncResult: true },
  setSubsystemLoggerLevel: { asyncResult: true },
});

export default LoggersActions;
