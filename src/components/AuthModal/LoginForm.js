import React, { useState } from 'react';
import './AuthModal.css';

const LoginForm = ({ switchType }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [keepLogin, setKeepLogin] = useState(false);

  const handleLogin = () => {
    console.log('이메일 로그인:', email, password, keepLogin);
  };

  return (
    <div className="login-container">
      <h2 className="login-title">로그인</h2>

      <input
        type="email"
        placeholder="이메일을 입력하세요"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        className="login-input"
      />

      <input
        type="password"
        placeholder="비밀번호를 입력하세요"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        className="login-input"
      />

      <div className="login-options">
        <label className="checkbox-wrap">
          <input
            type="checkbox"
            checked={keepLogin}
            onChange={(e) => setKeepLogin(e.target.checked)}
          />
          로그인 유지
        </label>
        <span className="find-link">비밀번호 찾기</span>
      </div>

      <button className="email-login-btn" onClick={handleLogin}>
        이메일로 로그인
      </button>

      <div className="login-bottom-links">
        <span className="link" onClick={() => switchType('signup')}>회원가입</span>
        <span className="divider">|</span>
        <span className="link">이메일 찾기</span>
      </div>
    </div>
  );
};

export default LoginForm;
