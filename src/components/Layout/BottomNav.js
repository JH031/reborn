import React from 'react';
import { NavLink } from 'react-router-dom';
import './BottomNav.css';

const IconHome = ({ className }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor">
    <path d="M3 10.5 12 3l9 7.5V21a1 1 0 0 1-1 1h-5v-6a3 3 0 0 0-6 0v6H4a1 1 0 0 1-1-1v-10.5z"
          strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const IconUser = ({ className }) => (
  <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor">
    <circle cx="12" cy="8" r="4" strokeWidth="1.8" />
    <path d="M4 21a8 8 0 0 1 16 0" strokeWidth="1.8" strokeLinecap="round" />
  </svg>
);

export default function BottomNav({ variant = 'minimal' }) {
  return (
    <>
      <div className="bn-spacer" aria-hidden="true" />
      <nav className={`bottom-nav ${variant}`}>
        <NavLink to="/" end className={({ isActive }) => `bn-item ${isActive ? 'active' : ''}`}>
          <IconHome className="bn-icon" />
          <span className="bn-label">홈</span>
        </NavLink>
        <NavLink to="/mypage" className={({ isActive }) => `bn-item ${isActive ? 'active' : ''}`}>
          <IconUser className="bn-icon" />
          <span className="bn-label">마이</span>
        </NavLink>
      </nav>
    </>
  );
}
