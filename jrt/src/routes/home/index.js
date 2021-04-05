import React from 'react';
import './style.css';
import {DiskUsage} from "../../components/DiskUsage";
import {TorrentsList} from "../../components/TorrentsList";
import AddButton from "../../components/AddButton";

const Home = () => (
	<div className='home'>
		<DiskUsage />
		<TorrentsList/>
		<AddButton/>
	</div>
);

export default Home;
