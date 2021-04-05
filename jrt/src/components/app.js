import React from 'react';
import Route from 'react-ui-basics/router/Route';
import Header from './header';
import Home from '../routes/home';
import Dialog from "./Dialog";

const App = () => (
    <div id="app">
        <Header/>
        <Route path="/"><Home path="/"/></Route>
        <Dialog/>
    </div>
)

export default App;
