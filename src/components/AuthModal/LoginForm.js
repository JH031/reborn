import React, { useState } from 'react';
import axios from 'axios';
import { useAuth } from '../../context/AuthContext';

const LoginForm = ({ switchType, onClose }) => {
  const [id, setId] = useState('');
  const [password, setPassword] = useState('');
  const { login } = useAuth();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!id || !password) {
      alert('아이디와 비밀번호를 모두 입력해주세요.');
      return;
    }

    try {
      // 1) 로그인 요청
      const res = await axios.post('http://localhost:8080/api/users/login', {
        userid: id,
        password,
      });

      // 2) 토큰 추출 (모든 케이스 커버)
      //    - 헤더 Authorization: Bearer <jwt>
      //    - 바디 { accessToken } | { token } | { jwt }
      const authHeader =
        res.headers?.authorization || res.headers?.Authorization || '';
      let token =
        (authHeader.startsWith('Bearer ') && authHeader.slice(7)) ||
        res.data?.accessToken ||
        res.data?.token ||
        res.data?.jwt ||
        '';

      if (!token) {
        // 서버가 토큰을 헤더/바디 어디에도 안 줬다면 401처럼 동작하므로 바로 안내
        throw new Error('서버에서 토큰을 받지 못했습니다.');
      }

      // 3) 로컬 저장 (ReviewListPage는 'token' 키를 읽음)
      localStorage.setItem('token', token);
      // (선택) 편의 키들도 함께 저장해두면 다른 화면에서도 재사용 쉬움
      localStorage.setItem('userid', id);
      if (res.data?.name) localStorage.setItem('username', res.data.name);

      // 4) 전역 상태 갱신 (필요한 정보+토큰 포함)
      const loginData = { ...res.data, userid: id, token };
      login(loginData);

      // 5) (선택) axios 전역에도 즉시 반영하고 싶으면 한 줄 추가
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;

      alert('로그인 성공!');
      onClose();
    } catch (error) {
      console.error('[login error]', error);
      if (error.response) {
        alert(
          `로그인 실패: ${error.response.data?.message || '아이디 또는 비밀번호를 확인해주세요.'}`
        );
      } else {
        alert(error.message || '로그인 중 오류가 발생했습니다.');
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
        <span className="link" onClick={() => switchType('findId')}>아이디 찾기</span>
      </div>
    </form>
  );
};

export default LoginForm;
