// src/pages/ChangePasswordPage.jsx
import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import Header from '../components/Layout/Header';
import BottomNav from '../components/Layout/BottomNav';
import FixedFrame from '../components/Layout/FixedFrame';
import './ChangePasswordPage.css';

// ✅ 공통 헬퍼: 응답 본문이 있으면 JSON 파싱, 없으면 null
const parseJsonIfAny = async (resp) => {
  const text = await resp.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
};

const ChangePasswordPage = () => {
  const { user } = useAuth();
  const [step, setStep] = useState(1);
  const [email, setEmail] = useState('');
  const [userid, setUserid] = useState('');
  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // 로그인 상태일 경우 기본값 채우기
  useEffect(() => {
    if (user) {
      setEmail(user.email || '');
      setUserid(user.userid || '');
    }
  }, [user]);

  // 1) 비밀번호 재설정 링크 요청
  const handleSendLink = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setMessage('');
    setError('');

    try {
      const resp = await fetch('http://localhost:8080/api/password/send-link', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userid, email }), // userid, email 필드 사용
      });

      const data = await parseJsonIfAny(resp);

      if (resp.ok) {
        setMessage('비밀번호 재설정 링크가 이메일로 발송되었습니다. 이메일을 확인해주세요.');
        setStep(2);
      } else {
        setError((data && data.message) || '링크 발송에 실패했습니다. 아이디와 이메일을 확인해주세요.');
      }
    } catch (err) {
      console.error('Send link error:', err);
      setError('서버와 통신 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setIsLoading(false);
    }
  };

  // 2) 토큰 검증
  const handleVerifyToken = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setMessage('');
    setError('');

    try {
      const resp = await fetch('http://localhost:8080/api/password/verify-token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token }),
      });

      const data = await parseJsonIfAny(resp);

      if (resp.ok) {
        setMessage('토큰이 성공적으로 검증되었습니다. 새 비밀번호를 입력해주세요.');
        setStep(3);
      } else {
        setError((data && data.message) || '유효하지 않은 토큰입니다.');
      }
    } catch (err) {
      console.error('Verify token error:', err);
      setError('서버와 통신 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  // 3) 새 비밀번호 설정
  const handleResetPassword = async (e) => {
    e.preventDefault();

    if (newPassword !== confirmPassword) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }

    setIsLoading(true);
    setMessage('');
    setError('');

    try {
      const resp = await fetch('http://localhost:8080/api/password/reset', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, newPassword }),
      });

      const data = await parseJsonIfAny(resp);

      if (resp.ok) {
        setMessage('비밀번호가 성공적으로 변경되었습니다.');
        setStep(4);
      } else {
        setError((data && data.message) || '비밀번호 변경에 실패했습니다.');
      }
    } catch (err) {
      console.error('Reset password error:', err);
      setError('서버와 통신 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="app-container">
      <FixedFrame>
        <Header />
        <main className="change-password-content">
          <h2>비밀번호 변경</h2>

          {message && <p className="message success">{message}</p>}
          {error && <p className="message error">{error}</p>}

          {step === 1 && (
            <form onSubmit={handleSendLink} className="password-form">
              <p>
                가입 시 사용한 아이디와 이메일 주소를 입력하시면, <br />
                비밀번호를 재설정할 수 있는 링크를 보내드립니다.
              </p>

              <div className="form-group">
                <label htmlFor="userid">아이디</label>
                <input
                  type="text"
                  id="userid"
                  value={userid}
                  onChange={(e) => setUserid(e.target.value)}
                  required
                  // ✅ 값이 있을 때만 readOnly
                  readOnly={!!user && !!userid}
                />
              </div>

              <div className="form-group">
                <label htmlFor="email">이메일</label>
                <input
                  type="email"
                  id="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  // ✅ 값이 있을 때만 readOnly
                  readOnly={!!user && !!email}
                />
              </div>

              <button type="submit" disabled={isLoading}>
                {isLoading ? '전송 중...' : '재설정 링크 받기'}
              </button>
            </form>
          )}

          {step === 2 && (
            <form onSubmit={handleVerifyToken} className="password-form">
              <p>이메일로 받은 인증 토큰을 입력해주세요.</p>
              <div className="form-group">
                <label htmlFor="token">인증 토큰</label>
                <input
                  type="text"
                  id="token"
                  value={token}
                  onChange={(e) => setToken(e.target.value)}
                  required
                />
              </div>
              <button type="submit" disabled={isLoading}>
                {isLoading ? '확인 중...' : '토큰 확인'}
              </button>
            </form>
          )}

          {step === 3 && (
            <form onSubmit={handleResetPassword} className="password-form">
              <p>새로 사용할 비밀번호를 입력해주세요.</p>
              <div className="form-group">
                <label htmlFor="newPassword">새 비밀번호</label>
                <input
                  type="password"
                  id="newPassword"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                />
              </div>
              <div className="form-group">
                <label htmlFor="confirmPassword">새 비밀번호 확인</label>
                <input
                  type="password"
                  id="confirmPassword"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                />
              </div>
              <button type="submit" disabled={isLoading}>
                {isLoading ? '변경 중...' : '비밀번호 변경'}
              </button>
            </form>
          )}

          {step === 4 && (
            <div className="completion-message">
              <h3>비밀번호 변경 완료</h3>
              <p>비밀번호가 성공적으로 변경되었습니다.</p>
            </div>
          )}
        </main>
        <BottomNav />
      </FixedFrame>
    </div>
  );
};

export default ChangePasswordPage;
