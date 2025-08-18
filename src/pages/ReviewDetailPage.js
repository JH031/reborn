// src/pages/ReviewDetailPage.jsx
import React, { useMemo, useState } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import BottomNav from '../components/Layout/BottomNav';
import FixedFrame from '../components/Layout/FixedFrame';
import axios from 'axios';
import './ReviewDetailPage.css';

const ReviewDetailPage = () => {
  const { studyId: paramStudyId } = useParams();
  const { state } = useLocation();
  const navigate = useNavigate();

  const [confidence, setConfidence] = useState('');
  const [posting, setPosting] = useState(false);


  const payload = useMemo(() => {
    return {
      studyId: state?.studyId ?? Number(paramStudyId),
      imageUrl: state?.imageUrl ?? null,
      reviewTurn: state?.reviewTurn ?? 1,
      stageDay: state?.stageDay ?? 1,
      title: state?.title ?? `학습 ${paramStudyId}`,
    };
  }, [state, paramStudyId]);

  const handleNextProblem = async () => {
    if (!payload.studyId) {
      alert('필요한 정보가 부족합니다.');
      navigate(-1);
      return;
    }
    if (!confidence) {
      alert('자기 확신 체크를 먼저 해주세요.');
      return;
    }

    const result = confidence === '확실함' ? 'UNDERSTOOD' : 'NOT_UNDERSTOOD';

    try {
      setPosting(true);
      const token = localStorage.getItem('token');
      await axios.post(`http://localhost:8080/api/studies/${payload.studyId}/review`,
        {},
        {
          params: { stageDay: payload.stageDay, result },
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      alert(`복습 완료! (${confidence} 선택)`);
      navigate('/mypage/review');
    } catch (e) {
      console.error(e);
      alert('이해도 결과 전송에 실패했습니다.');
    } finally {
      setPosting(false);
    }
  };

  return (
    <div className="app-container">
      <FixedFrame>
        <header className="simple-header">
          <button onClick={() => navigate(-1)} className="back-button">←</button>
          <h1>문제 다시 풀기</h1>
        </header>

        <main className="review-detail-content">
          {payload.imageUrl ? (
            <div className="problem-image-container">
              <img src={payload.imageUrl} alt={payload.title} />
            </div>
          ) : (
            <p className="review-list-description">이미지가 없어요. 학습 제목: {payload.title}</p>
          )}

          <div className="review-controls">
            <div className="confidence-check">
              <h4>자기 확신 체크</h4>
              <div className="radio-group">
                <label className={confidence === '확실함' ? 'checked' : ''}>
                  <input
                    type="radio"
                    name="confidence"
                    value="확실함"
                    onChange={(e) => setConfidence(e.target.value)}
                  />
                  확실함
                </label>
                <label className={confidence === '모름' ? 'checked' : ''}>
                  <input
                    type="radio"
                    name="confidence"
                    value="모름"
                    onChange={(e) => setConfidence(e.target.value)}
                  />
                  모름
                </label>
              </div>
            </div>

            <button
              className="next-problem-btn"
              onClick={handleNextProblem}
              disabled={posting}
            >
              {posting ? '전송 중…' : '다음 문제로'}
            </button>
          </div>
        </main>

        <BottomNav />
      </FixedFrame>
    </div>
  );
};

export default ReviewDetailPage;
