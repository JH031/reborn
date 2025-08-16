import React from 'react';
import { Link } from 'react-router-dom'; // ❗ Link import
import './BottomNav.css';

const BottomNav = () => {
  return (
    <div className="bottom-nav">
      {/* ❗ a 태그를 Link 태그로 변경 */}
      <Link to="/">🏠</Link>
      <Link to="/mypage">👤</Link>
    </div>
  );
};

export default BottomNav;