import React from 'react';
import ReactDOM from 'react-dom';
import './style/index.css';
import App from './components/app';
import SetupNetwork from './network/SetupNetwork'
// import "react-ui-basics/Table.css";

SetupNetwork();

ReactDOM.render(<App/>, document.getElementById('root'));