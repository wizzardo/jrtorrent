import { h } from 'preact';
import NavLink from 'react-ui-basics/router/NavLink';
import './style.css';

const Header = () => (
	<header class="header">
		<h1>Preact App</h1>
		<nav>
			<NavLink activeClassName="active" href="/">Home</NavLink>
			<NavLink activeClassName="active" href="/profile">Me</NavLink>
			<NavLink activeClassName="active" href="/profile/john">John</NavLink>
		</nav>
	</header>
);

export default Header;
