import React from 'react';
import ReactDOM from 'react-dom';
import './style/index.css';
import App from './components/app';
import SetupNetwork from './network/SetupNetwork'

SetupNetwork();

ReactDOM.render(<App/>, document.getElementById('root'));