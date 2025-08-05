// src/App.js
import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import MainPage from './pages/MainPage';

const App = () => {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<MainPage />} />
        {/* 추후 마이페이지, 로그인, 회원가입도 여기에 추가 */}
      </Routes>
    </Router>
  );
};

export default App;
