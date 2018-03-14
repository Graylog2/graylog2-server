import Reflux from 'reflux';

const ContentPackStores = Reflux.createStore({
  init() {
    this.contentPacks = [
      { id: '1', title: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles', version: '1.0', states: ['installed', 'edited'], },
      { id: '2', title: 'Rails Log Patterns', summary: 'Patterns to retreive rails production logs', version: '2.1', states: [], },
      { id: '3', title: 'Backup Content Pack', summary: '', version: '3.0', states: ['installed'], },
      { id: '4', title: 'SSH Archive', summary: 'A crypted backup over ssh.', version: '3.4', states: ['error'], },
      { id: '5', title: 'FTP Backup', summary: 'Fast but insecure backup', version: '1.0', states: ['installed', 'updatable'], },
      { id: '6', title: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles', version: '1.0', states: ['installed', 'edited'], },
      { id: '7', title: 'Rails Log Patterns', summary: 'Patterns to retreive rails production logs', version: '2.1', states: [], },
      { id: '8', title: 'Backup Content Pack', summary: '', version: '3.0', states: ['installed'] },
      { id: '9', title: 'SSH Archive', summary: 'A crypted backup over ssh.', version: '3.4', states: ['error'] },
      { id: '10', title: 'FTP Backup', summary: 'Fast but insecure backup', version: '1.0', states: ['installed', 'updatable'], },
      { id: '11', title: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles', version: '1.0', states: ['installed', 'edited'], },
      { id: '12', title: 'Rails Log Patterns', summary: 'Patterns to retreive rails production logs', version: '2.1', states: [], },
      { id: '13', title: 'Backup Content Pack', summary: '', version: '3.0', states: ['installed']},
      { id: '14', title: 'SSH Archive', summary: 'A crypted backup over ssh.', version: '3.4', states: ['error'] },
      { id: '15', title: 'FTP Backup', summary: 'Fast but insecure backup', version: '1.0', states: ['installed', 'updatable'], },
    ];
    this.readme = `
# Active Directory Auditing Content Pack

Tested with nxLog/Windows 2008R2 Domain Controllers/Graylog 1.2

This content pack provides several useful dashboards for auditing Active Directory events:

*   DNS Object Summary - DNS Creations, Deletions
*   Group Object Summary - Group Creations, Modifications, Deletions, Membership Changes
*   User Object Summary - Account Creations, Deletions, Modifications, Lockouts, Unlocks
*   Computer Object Summary - (in progress)
*   Logon Summary - Failed Authentication Attempts, Interactive Logins
`;

    this.contentPack = {
      '1.0': { id: '1', title: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles',
        version: '1.0', states: ['installed', 'edited'], vendor: 'graylog.org <info@graylog.org>',
        description: this.readme,
        url: "https://github.com/graylog2/graylog2-server",
        constraints: [
          {type: "Server", name: "graylog", version: "3.0", fullfilled: true},
        ],
      },
      '2.0': { id: '1', title: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles',
        version: '2.0', states: ['installed', 'edited'], vendor: 'graylog.org <info@graylog.org>',
        description: this.readme,
        url: "https://github.com/graylog2/graylog2-server",
        constraints: [
          {type: "Server", name: "graylog", version: "3.0", fullfilled: true},
          {type: "Plugin", name: "IntelThreadPlugin", version: "1.0", fullfilled: false},
        ],
      },
    };

  },

  get(contentPackId) {
    const promise = new Promise((resolve) => {
      setTimeout(() => {
        resolve(this.contentPack);
      }, 300);
    });
    return promise;
  },
  list() {
    const promise = new Promise((resolve) => {
      setTimeout(() => {
        resolve(this.contentPacks);
      }, 300);
    });
    return promise;
  },
});

export default ContentPackStores;
