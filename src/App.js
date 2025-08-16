import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import MainPage from './pages/MainPage';
import MyPage from './pages/MyPage';
import ReviewListPage from './pages/ReviewListPage.js';
import ReviewDetailPage from './pages/ReviewDetailPage';
import { AuthProvider } from './context/AuthContext';
import EditProfilePage from './pages/EditProfilePage';
// ❗ 1. 비밀번호 변경 페이지 import
import ChangePasswordPage from './pages/ChangePasswordPage';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/" element={<MainPage />} />
          <Route path="/chat/:problemId" element={<MainPage />} />
          <Route path="/mypage" element={<MyPage />} />
          <Route path="/mypage/review" element={<ReviewListPage />} />
          <Route path="/review/:problemId" element={<ReviewDetailPage />} />
          <Route path="/mypage/edit-profile" element={<EditProfilePage />} />
          {/* ❗ 2. 비밀번호 변경 페이지를 위한 라우트 추가 */}
          <Route path="/mypage/change-password" element={<ChangePasswordPage />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
