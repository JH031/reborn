// src/components/Layout/Header.js

import React from 'react';
import { useAuth } from '../../context/AuthContext';
import './Header.css';

const Header = ({ onLoginClick, onMenuClick }) => { // onMenuClick prop 추가
    const { user, logout } = useAuth();

    return (
        <div className="header">
            <div className="header-left">
                <div className="menu-icon" onClick={onMenuClick}>☰</div>
                <div className="logo">REBORN</div>
            </div>
            <div className="header-right">
                {user ? (
                    <div className="user-info">
                        <span>{user.name}님 환영합니다!</span>
                        <button className="logout-btn" onClick={logout}>로그아웃</button>
                    </div>
                ) : (
                    <button className="login-btn" onClick={onLoginClick}>로그인</button>
                )}
            </div>
        </div>
    );
};

export default Header;