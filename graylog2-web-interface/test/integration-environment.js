const JSDomEnvironment = require('jest-environment-jsdom');

class IntegrationEnvironment extends JSDomEnvironment {
  constructor(config) {
    super(config);
  }

  async setup() {
    await super.setup();
  }

  async teardown() {
    await super.teardown();
  }

  runScript(script) {
    return super.runScript(script);
  }
}

module.exports = IntegrationEnvironment;
