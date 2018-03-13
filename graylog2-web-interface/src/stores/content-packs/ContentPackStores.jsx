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
