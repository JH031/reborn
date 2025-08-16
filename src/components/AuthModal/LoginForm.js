// src/components/AuthModal/LoginForm.js

import React, { useState } from 'react';
import axios from 'axios';
import { useAuth } from '../../context/AuthContext';

const LoginForm = ({ switchType, onClose }) => {
  const [id, setId] = useState(''); // 이 변수에 사용자가 입력한 아이디(userid)가 담겨 있습니다.
  const [password, setPassword] = useState('');
  const { login } = useAuth();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!id || !password) {
      alert("아이디와 비밀번호를 모두 입력해주세요.");
      return;
    }
    try {
      const response = await axios.post('http://localhost:8080/api/users/login', { // 프록시 사용을 위해 주소 수정
        userid: id,
        password: password,
      });
      
      // ❗ 핵심: 서버 응답 데이터에 사용자가 입력한 아이디(id)를 'userid'라는 키로 추가합니다.
      const loginData = { ...response.data, userid: id };
      
      // ❗ 수정된 데이터를 login 함수에 전달합니다.
      login(loginData); 
      
      alert('로그인 성공!');
      onClose();

    } catch (error) {
      if (error.response) {
        alert(`로그인 실패: ${error.response.data.message || '아이디 또는 비밀번호를 확인해주세요.'}`);
      } else {
        alert('로그인 중 오류가 발생했습니다.');
      }
    }
  };

  return (
    <form onSubmit={handleSubmit} className="login-container">
      <h2 className="login-title">로그인</h2>
      <input
        type="text"
        placeholder="아이디를 입력하세요"
        value={id}
        onChange={(e) => setId(e.target.value)}
        className="login-input"
      />
      <input
        type="password"
        placeholder="비밀번호를 입력하세요"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        className="login-input"
      />
      <button type="submit" className="email-login-btn">로그인</button>
      <div className="login-bottom-links">
        <span className="link" onClick={() => switchType('signup')}>회원가입</span>
      </div>
    </form>
  );
};

export default LoginForm;