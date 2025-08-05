// src/components/Layout/Header.js
import React from 'react';
import './Header.css';

const Header = ({ onLoginClick, onSignupClick }) => {
  return (
    <div className="header">
      <div className="logo">REBORN</div>
      <div className="auth-links">
        <button className="login-btn" onClick={onLoginClick}>로그인</button>
    </div>

    </div>
  );
};

export default Header;
