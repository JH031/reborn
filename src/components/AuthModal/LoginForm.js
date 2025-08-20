import React, { useState } from 'react';
import axios from 'axios';
import { useAuth } from '../../context/AuthContext';
import { useNavigate } from 'react-router-dom';

const LoginForm = ({ switchType, onClose }) => {
  const [id, setId] = useState('');
  const [password, setPassword] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!id || !password) {
      alert('아이디와 비밀번호를 모두 입력해주세요.');
      return;
    }

    try {
      const res = await axios.post('http://localhost:8080/api/users/login', {
        userid: id,
        password,
      });

      const authHeader = res.headers?.authorization || res.headers?.Authorization || '';
      let token =
        (authHeader.startsWith('Bearer ') && authHeader.slice(7)) ||
        res.data?.accessToken ||
        res.data?.token ||
        res.data?.jwt ||
        '';

      if (!token) throw new Error('서버에서 토큰을 받지 못했습니다.');

      localStorage.setItem('token', token);
      localStorage.setItem('userid', id);
      if (res.data?.name) localStorage.setItem('username', res.data.name);

      const loginData = { ...res.data, userid: id, token };
      login(loginData);

      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;

      alert('로그인 성공!');
      onClose();
    } catch (error) {
      console.error('[login error]', error);
      if (error.response) {
        alert(`로그인 실패: ${error.response.data?.message || '아이디 또는 비밀번호를 확인해주세요.'}`);
      } else {
        alert(error.message || '로그인 중 오류가 발생했습니다.');
      }
    }
  };

  const goResetPassword = () => {
    if (onClose) onClose();
    navigate('/reset-password');
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
        <span className="link" onClick={() => switchType('findId')}>아이디 찾기</span>
        <span className="link" onClick={goResetPassword}>비밀번호 재설정</span>
      </div>
    </form>
  );
};

export default LoginForm;