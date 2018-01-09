import JvmInfoStore from './system/JvmInfoStore';
import SessionStore from './sessions/SessionStore';
import SystemInfoStore from './system/SystemInfoStore';

class RootStore {
  constructor() {
    this.jvmInfoStore = new JvmInfoStore();
    this.sessionStore = new SessionStore();
    this.systemInfoStore = new SystemInfoStore();
  }
}

export default new RootStore();
