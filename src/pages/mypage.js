// src/pages/MyPage.js
import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import Header from '../components/Layout/Header';
import BottomNav from '../components/Layout/BottomNav';
import FixedFrame from '../components/Layout/FixedFrame';
import { useAuth } from '../context/AuthContext';
import ChatHistorySidebar from '../components/Layout/ChatHistorySidebar';
import AuthModal from '../components/AuthModal/AuthModal'; // ★ 모달 import
import './MyPage.css';

const MyPage = () => {
  const { user, logout } = useAuth();
  const [modalType, setModalType] = useState(null); // 'login' | 'signup' | null
  const [isSidebarOpen, setSidebarOpen] = useState(false);
  

  return (
    <div className="app-container">
        {isSidebarOpen && <div className="backdrop" onClick={() => setSidebarOpen(false)}></div>}
      <ChatHistorySidebar isOpen={isSidebarOpen} onClose={() => setSidebarOpen(false)} />
      <FixedFrame>
        <Header 
            onLoginClick={() => setModalType('login')} 
            onMenuClick={() => setSidebarOpen((v) => !v)}
            /> {/* ★ prop 전달 */}
        
        <main className="mypage-content">
          <div className="profile-header">
            <h2>마이페이지</h2>
            {user && <p>{user.name}님, 환영합니다!</p>}
          </div>

          <div className="mypage-menu">
            <div className="menu-section">
              <h3>학습 관리</h3>
              <Link to="/mypage/review" className="menu-item">복습하기</Link>
            </div>

            <div className="menu-section">
              <h3>계정 관리</h3>
              <Link to="/mypage/edit-profile" className="menu-item">회원정보 수정</Link>
              <Link to="/mypage/change-password" className="menu-item">비밀번호 변경</Link>
              <div className="menu-item" onClick={logout}>로그아웃</div>
            </div>
          </div>
        </main>
        <BottomNav />
      </FixedFrame>

      {/* ★ 모달 조건부 렌더 */}
      {modalType && (
        <AuthModal
          type={modalType}
          onClose={() => setModalType(null)}
          switchType={setModalType}
        />
      )}
    </div>
  );
};

export default MyPage;