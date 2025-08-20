import React from 'react';
import { useAuth } from '../../context/AuthContext';
import './Header.css';

const Header = ({ onLoginClick, onMenuClick }) => {
  const { user } = useAuth(); 

  return (
    <div className="header">
      <div className="header-left">
        <div className="menu-icon" onClick={onMenuClick}>☰</div>
        <div className="logo">REBORN</div>
      </div>
      <div className="header-right">
        {!user && (
          <button className="login-btn" type="button" onClick={onLoginClick}>
            로그인
          </button>
        )}
        {user && (
          <div className="user-info">
            <span>{user.name}님 환영합니다!</span>
          </div>
        )}
      </div>
    </div>
  );
};

export default Header;
