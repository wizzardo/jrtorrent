import React from 'react';
import Route from 'react-ui-basics/router/Route';
import Header from './header';
import Home from '../routes/home';
import Profile from '../routes/profile';

const App = () => (
    <div id="app">
        <Header/>

        <Route path="/"><Home path="/"/></Route>
        <Route path="/profile"><Profile user="me"/></Route>
        <Route path="/profile/:user"><Profile/></Route>

    </div>
)

export default App;
