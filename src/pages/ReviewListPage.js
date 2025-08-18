// src/pages/ReviewListPage.jsx
import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import BottomNav from '../components/Layout/BottomNav';
import FixedFrame from '../components/Layout/FixedFrame';
import axios from 'axios';
import './ReviewListPage.css';

function todayYMD() {
  const d = new Date();
  const m = `${d.getMonth() + 1}`.padStart(2, '0');
  const day = `${d.getDate()}`.padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
}

const OFFSETS = [1, 4, 7, 14, 30];

const ReviewListPage = () => {
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState('');

  useEffect(() => {
    const fetchDue = async () => {
      try {
        setLoading(true);
        setErr('');

        // 1) 로그인 때 저장한 토큰 읽기 (여러 키 대응)
        const token =
          localStorage.getItem('token') ||
          localStorage.getItem('accessToken') ||
          localStorage.getItem('jwt');

        // 2) 토큰이 없으면 로그인 유도
        if (!token) {
          setErr('로그인이 필요합니다. 다시 로그인해 주세요.');
          return;
        }

        // 3) Authorization 헤더를 직접 부착해서 호출
        const res = await axios.get('http://localhost:8080/api/reviews/due', {
          params: { date: todayYMD(), includeOverdue: true },
          headers: { Authorization: `Bearer ${token}` },
        });

        // 4) 응답 정규화
        const list = Array.isArray(res.data) ? res.data : [];
        const today = new Date(todayYMD());

        const normalized = list.map((it) => {
          const due = it.nextReviewDate ? new Date(it.nextReviewDate) : today;
          const diffDays = Math.floor((today - due) / (1000 * 60 * 60 * 24));
          const daysAgo = isNaN(diffDays) || diffDays < 0 ? 0 : diffDays;

          const idx = Number(it.stageIndex ?? 1);
          const reviewTurn = idx === 0 ? 1 : idx;       // 0이면 1회차로 보정
          const stageDay = OFFSETS[reviewTurn - 1] ?? 1;

          return {
            reviewProgressId: it.reviewProgressId,
            studyId: it.userStudyId,
            title: it.contentTitle ?? '복습',
            reviewTurn,
            stageDay,
            daysAgo,
            imageUrl: it.imageUrl || null,
          };
        });

        setItems(normalized);
      } catch (e) {
        console.error(e);
        if (e?.response?.status === 401) {
          setErr('로그인이 필요합니다. 다시 로그인해 주세요.');
          // 필요하면 자동 이동: navigate('/login');
        } else {
          setErr('복습 목록을 불러오지 못했습니다.');
        }
      } finally {
        setLoading(false);
      }
    };

    fetchDue();
  }, [navigate]);

  return (
    <div className="app-container">
      <FixedFrame>
        <header className="simple-header">
          <button onClick={() => navigate(-1)} className="back-button">←</button>
          <h1>오늘의 복습</h1>
        </header>
        <main className="review-list-content">
          {loading && <p className="review-list-description">불러오는 중…</p>}
          {err && <p className="error-text">{err}</p>}
          {!loading && !err && items.length === 0 && (
            <p className="review-list-description">오늘 복습할 문제가 없어요.</p>
          )}

          <div className="review-card-list">
            {items.map((item) => (
              <Link
                to={`/review/${item.studyId}`}
                key={`${item.reviewProgressId}-${item.studyId}`}
                className="review-card"
                state={{
                  studyId: item.studyId,
                  reviewProgressId: item.reviewProgressId,
                  imageUrl: item.imageUrl,
                  reviewTurn: item.reviewTurn,
                  stageDay: item.stageDay,
                  title: item.title,
                }}
              >
                <div className="card-info">
                  <span className="card-subject">복습</span>
                  <h3 className="card-concept">{item.title}</h3>
                  <p className="card-meta">
                    {item.reviewTurn}회차 복습 ({item.daysAgo}일 전)
                  </p>
                </div>
                {item.imageUrl && (
                  <img src={item.imageUrl} alt={item.title} className="card-thumbnail" />
                )}
              </Link>
            ))}
          </div>
        </main>
        <BottomNav />
      </FixedFrame>
    </div>
  );
};

export default ReviewListPage;
