export class Constants {
  static TRANSPORT_HTTP = 'TRANSPORT_HTTP';
  static TRANSPORT_AUTO = 'TRANSPORT_AUTO';
  static TRANSPORT_WS = 'TRANSPORT_WS';
}

const config = {
  base: '',
  transport: Constants.TRANSPORT_AUTO,
  showStats: true,
  showLog: true,
  token: null,
  platform: '',
  platformVersion: '',
  appVersion: '',
};

export const getWsUrl = () => {
  let base = config.base;
  let isHttps = base.indexOf("https:") === 0 || (window.location && window.location.protocol === 'https:');
  if (base.indexOf("https:") === 0)
    base = base.substring(6, base.length);
  if (base.indexOf("http:") === 0)
    base = base.substring(5, base.length);

  let result = (isHttps ? 'wss:' : 'ws:') + base + '/ws';

  result += `?token=${config.token}`;

  return result;
};
export const showLog = (show) => show !== undefined ? (config.showLog = show) : config.showLog;
export const showStats = (show) => show !== undefined ? (config.showStats = show) : config.showStats;
export const getConfig = () => config;
