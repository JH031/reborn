import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import FixedFrame from '../components/Layout/FixedFrame';
import BottomNav from '../components/Layout/BottomNav';
import { useAuth } from '../context/AuthContext';
import axios from 'axios';
import './EditProfilePage.css';

const EditProfilePage = () => {
  const navigate = useNavigate();
  const { user, token, login } = useAuth();

  const [email, setEmail] = useState('');
  const [grade, setGrade] = useState('');
  const [school, setSchool] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (user) {
      setEmail(user.email || '');
      setGrade(user.grade || '');
      setSchool(user.school || '');
    }
  }, [user]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!user) return;

    setError('');
    setSaving(true);
    try {
      const response = await axios.patch(
        `http://localhost:8080/api/users/${user.id}/profile`,
        { email, grade: parseInt(grade, 10), school },
        { headers: { Authorization: `Bearer ${token}` } }
      );

      // ✅ 성공 시 컨텍스트 갱신 (네 컨텍스트 시그니처에 맞춰 유지)
      const updatedUser = { ...user, ...response.data };
      login({ token, ...updatedUser });

      alert('회원정보가 성공적으로 수정되었습니다.');
      navigate('/mypage');
    } catch (err) {
      console.error('회원정보 수정 실패:', err);
      setError('정보 수정에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="app-container">
      <FixedFrame>
        <header className="simple-header">
          <button onClick={() => navigate(-1)} className="back-button">←</button>
          <h1>회원정보 수정</h1>
        </header>

        <main className="profile-plain-wrap">
          {error && <p className="banner banner--error">{error}</p>}

          {/* HERO (흰 배경, 카드 없음) */}
          <div className="plain-hero">
            <div className="avatar-badge" aria-hidden>👤</div>
            <p className="hero-text">
              이메일, 학년, 학교급 정보를 수정할 수 있어요.
            </p>
          </div>

          {/* BODY */}
          <div className="plain-body">
            <form onSubmit={handleSubmit} className="form">
              <div className="form-group">
                <label htmlFor="email">이메일</label>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="grade">학년</label>
                <input
                  id="grade"
                  type="number"
                  min="1"
                  max="6"
                  value={grade}
                  onChange={(e) => setGrade(e.target.value)}
                  required
                />
                <p className="field-hint">※ 초·중·고 공통으로 1~6 입력</p>
              </div>

              <div className="form-group">
                <label htmlFor="school">학교급</label>
                <select
                  id="school"
                  value={school}
                  onChange={(e) => setSchool(e.target.value)}
                  required
                >
                  <option value="">선택하세요</option>
                  <option value="ELEMENTARY">초등학교</option>
                  <option value="MIDDLE">중학교</option>
                  <option value="HIGH">고등학교</option>
                </select>
              </div>

              <button type="submit" className="btn-primary" disabled={saving}>
                {saving ? '저장 중…' : '저장하기'}
              </button>
            </form>
          </div>
        </main>

        <BottomNav />
      </FixedFrame>
    </div>
  );
};

export default EditProfilePage;
