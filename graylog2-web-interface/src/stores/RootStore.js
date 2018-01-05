import SystemInfoStore from './system/SystemInfoStore';

class RootStore {
  constructor() {
    this.systemInfoStore = new SystemInfoStore();
  }
}

export default new RootStore();
