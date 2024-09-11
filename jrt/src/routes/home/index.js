import React from 'react';
import './style.css';
import {DiskUsage} from "../../components/DiskUsage";
import {TorrentsList} from "../../components/TorrentsList";
import AddButton from "../../components/AddButton";
import {FolderFilter} from "../../components/FolderFilter.js";

const Home = () => (
	<div className='home'>
		<div className={"topbar"}>
			<DiskUsage />
			<FolderFilter/>
		</div>
		<TorrentsList/>
		<AddButton/>
	</div>
);

export default Home;
