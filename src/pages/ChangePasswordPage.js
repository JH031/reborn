// src/pages/ChangePasswordPage.js
import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import BottomNav from '../components/Layout/BottomNav';
import FixedFrame from '../components/Layout/FixedFrame';
import { useNavigate } from 'react-router-dom';
import './ChangePasswordPage.css';

const parseJsonIfAny = async (resp) => {
  const text = await resp.text();
  if (!text) return null;
  try { return JSON.parse(text); } catch { return null; }
};

// 전체 URL에서 token 파라미터만 뽑아주는 유틸
const extractToken = (value) => {
  if (!value) return '';
  try {
    const u = new URL(value);
    const t = u.searchParams.get('token');
    if (t) return t;
  } catch (_) {}
  const m = value.match(/token=([^&\s]+)/i);
  if (m) return m[1];
  return value.trim();
};

const ChangePasswordPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [step, setStep] = useState(1);
  const [email, setEmail] = useState('');
  const [userid, setUserid] = useState('');
  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (user) {
      setEmail(user.email || '');
      setUserid(user.userid || '');
    }
  }, [user]);

  const handleSendLink = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');
    try {
      const resp = await fetch('http://localhost:8080/api/password/send-link', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userid, email }),
      });
      const data = await parseJsonIfAny(resp);
      if (resp.ok) {
        setStep(2);
      } else {
        setError((data && data.message) || '아이디/이메일을 확인해주세요.');
      }
    } catch {
      setError('서버 통신 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleVerifyToken = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');
    try {
      const resp = await fetch('http://localhost:8080/api/password/verify-token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token }),
      });
      const data = await parseJsonIfAny(resp);
      if (resp.ok) {
        setStep(3);
      } else {
        setError((data && data.message) || '유효하지 않은 토큰입니다.');
      }
    } catch {
      setError('서버 통신 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }
    setIsLoading(true);
    setError('');
    try {
      const resp = await fetch('http://localhost:8080/api/password/reset', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, newPassword }),
      });
      const data = await parseJsonIfAny(resp);
      if (resp.ok) {
        setStep(4);
      } else {
        setError((data && data.message) || '비밀번호 변경에 실패했습니다.');
      }
    } catch {
      setError('서버 통신 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const helperByStep = {
    1: '가입 아이디와 이메일을 입력하면 비밀번호 재설정 링크를 보내드려요.',
    2: '이메일에 받은 링크 주소의 token= 뒤 문자열만 입력하세요.\n전체 링크를 붙여넣어도 자동으로 추출해 드립니다.',
    3: '새 비밀번호를 입력하고 확인해 주세요.',
    4: '비밀번호가 변경되었습니다. 새로운 비밀번호로 로그인하세요.',
  };

  return (
    <div className="app-container">
      <FixedFrame>
        <header className="simple-header">
          <button onClick={() => navigate(-1)} className="back-button">←</button>
          <h1>비밀번호 변경</h1>
        </header>

        <main className="pw-plain-wrap">
          {error && <p className="banner banner--error">{error}</p>}

          <div className="plain-hero">
            <div className="lock-badge" aria-hidden>🔒</div>
            <p className="hero-text">{helperByStep[step]}</p>
            {/* role="list" 제거 */}
            <ol className="stepper">
              <li className={`step ${step >= 1 ? 'active' : ''}`}>이메일</li>
              <li className={`step ${step >= 2 ? 'active' : ''}`}>토큰</li>
              <li className={`step ${step >= 3 ? 'active' : ''}`}>새 비밀번호</li>
            </ol>
          </div>

          <div className="plain-body">
            {step === 1 && (
              <form onSubmit={handleSendLink} className="form">
                <div className="form-group">
                  <label htmlFor="userid">아이디</label>
                  <input
                    type="text"
                    id="userid"
                    value={userid}
                    onChange={(e) => setUserid(e.target.value)}
                    required
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
                    readOnly={!!user && !!email}
                  />
                </div>
                <button type="submit" className="btn-primary" disabled={isLoading}>
                  {isLoading ? '전송 중…' : '재설정 링크 받기'}
                </button>
              </form>
            )}

            {step === 2 && (
              <form onSubmit={handleVerifyToken} className="form">
                <div className="form-group">
                  <label htmlFor="token">인증 토큰</label>
                  <input
                    type="text"
                    id="token"
                    value={token}
                    placeholder="예: xJHWqA_2cMy5J3KTOUL..."
                    onChange={(e) => setToken(extractToken(e.target.value))}
                    required
                  />
                  <p className="field-hint">
                    이메일에 적힌 링크에서 <code>token=</code> 뒤에 오는 문자열만 입력하세요.
                    전체 링크를 붙여넣어도 <strong>자동으로 토큰만 추출</strong>됩니다.<br />
                    예) <span className="mono">…/reset-password?token=abcd123</span> → <span className="mono">abcd123</span>
                  </p>
                </div>
                <button type="submit" className="btn-primary" disabled={isLoading}>
                  {isLoading ? '확인 중…' : '토큰 확인'}
                </button>
              </form>
            )}

            {step === 3 && (
              <form onSubmit={handleResetPassword} className="form">
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
                <button type="submit" className="btn-primary" disabled={isLoading}>
                  {isLoading ? '변경 중…' : '비밀번호 변경'}
                </button>
              </form>
            )}

            {step === 4 && (
              <div className="done">
                <h3>비밀번호 변경 완료</h3>
                <button className="btn-primary" onClick={() => navigate('/mypage')}>
                  마이페이지로
                </button>
              </div>
            )}
          </div>
        </main>

        <BottomNav />
      </FixedFrame>
    </div>
  );
};

export default ChangePasswordPage;
