import JvmInfoStore from './system/JvmInfoStore';
import SystemInfoStore from './system/SystemInfoStore';

class RootStore {
  constructor() {
    this.jvmInfoStore = new JvmInfoStore();
    this.systemInfoStore = new SystemInfoStore();
  }
}

export default new RootStore();
