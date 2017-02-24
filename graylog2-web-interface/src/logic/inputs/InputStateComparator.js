class InputStateComparator {
  constructor() {
    this.mapping = {
      CREATED: 0,
      INITIALIZED: 1,
      INVALID_CONFIGURATION: 2,
      STARTING: 3,
      RUNNING: 4,
      FAILED: 2,
      STOPPING: 1,
      STOPPED: 0,
      TERMINATED: 0,
    };
  }

  compare(state1, state2) {
    return this.mapping[state1.toUpperCase()] - this.mapping[state2.toUpperCase()];
  }
}

export default InputStateComparator;
