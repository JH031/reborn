// src/components/AuthModal/FindIdForm.js
import React, { useState } from 'react';
import axios from 'axios';

const FindIdForm = ({ switchType }) => {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);

  const handleFindId = async (e) => {
    e.preventDefault();
    if (!name || !email) {
      alert('이름과 이메일을 모두 입력해주세요.');
      return;
    }
    try {
      setLoading(true);
      const res = await axios.post('http://localhost:8080/api/users/find-id', {
        name,
        email,
      });
      const userid = res?.data?.userid;
      if (userid) {
        try {
          await navigator.clipboard.writeText(userid);
          alert(`아이디: ${userid}\n(클립보드에 복사되었습니다)`);
        } catch {
          alert(`아이디: ${userid}`);
        }
        switchType('login');
      } else {
        alert('일치하는 아이디를 찾지 못했습니다.');
      }
    } catch (error) {
      if (error.response?.status === 404) {
        alert('일치하는 계정이 없습니다. 이름/이메일을 다시 확인해주세요.');
      } else {
        alert('아이디 찾기 중 오류가 발생했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleFindId} className="login-container">
      <h2 className="login-title">아이디 찾기</h2>
      <input
        type="text"
        placeholder="이름을 입력하세요"
        value={name}
        onChange={(e) => setName(e.target.value)}
        className="login-input"
      />
      <input
        type="email"
        placeholder="이메일을 입력하세요"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        className="login-input"
      />
      <button type="submit" className="email-login-btn" disabled={loading}>
        {loading ? '조회 중...' : '아이디 찾기'}
      </button>
      <div className="login-bottom-links">
        <span className="link" onClick={() => switchType('login')}>로그인으로 돌아가기</span>
      </div>
    </form>
  );
};

export default FindIdForm;
