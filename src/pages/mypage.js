import React from 'react';
import { Link } from 'react-router-dom'; // ❗ Link import
import Header from '../components/Layout/Header';
import BottomNav from '../components/Layout/BottomNav';
import FixedFrame from '../components/Layout/FixedFrame';
import { useAuth } from '../context/AuthContext';
import './MyPage.css';

const MyPage = () => {
    const { user, logout } = useAuth();

    return (
        <div className="app-container">
            <FixedFrame>
                <Header />
                <main className="mypage-content">
                    <div className="profile-header">
                        <h2>마이페이지</h2>
                        {user && <p>{user.name}님, 환영합니다!</p>}
                    </div>

                    <div className="mypage-menu">
                        <div className="menu-section">
                            <h3>학습 관리</h3>
                            <Link to="/mypage/review" className="menu-item">복습하기</Link>
                            <div className="menu-item">취약점 피드백</div>
                        </div>

                        <div className="menu-section">
                            <h3>계정 관리</h3>
                            <Link to="/mypage/edit-profile" className="menu-item">회원정보 수정</Link>
                            <Link to="/mypage/change-password" className="menu-item">비밀번호 변경</Link>                            <div className="menu-item" onClick={logout}>로그아웃</div>
                        </div>
                    </div>
                </main>
                <BottomNav />
            </FixedFrame>
        </div>
    );
};

export default MyPage;