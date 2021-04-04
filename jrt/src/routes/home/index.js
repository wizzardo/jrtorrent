import React from 'react';
import './style.css';
import {DiskUsage} from "../../components/DiskUsage";

const Home = () => (
	<div className='home'>
		<DiskUsage />
		<h1>Home </h1>
		<p>This is the Home component.</p>
	</div>
);

export default Home;
