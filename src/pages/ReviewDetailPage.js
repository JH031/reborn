import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import BottomNav from '../components/Layout/BottomNav';
import FixedFrame from '../components/Layout/FixedFrame';
import './ReviewDetailPage.css';

// 임시 데이터 (원래는 problemId로 API 호출해서 가져와야 함)
const mockProblemDetail = {
    problemId: 1,
    imageUrl: 'https://via.placeholder.com/800x400.png?text=Problem+Image+1',
    correctAnswer: '정답은 1입니다. 나눗셈을 먼저 계산해야 합니다.',
};

const ReviewDetailPage = () => {
    const { problemId } = useParams(); // URL에서 problemId 가져오기
    const navigate = useNavigate();
    const [isAnswerVisible, setAnswerVisible] = useState(false);
    const [confidence, setConfidence] = useState('');

    const handleNextProblem = () => {
        if (!confidence) {
            alert('자기 확신 체크를 먼저 해주세요.');
            return;
        }
        // 실제로는 다음 문제로 이동하는 로직이 필요
        alert(`${problemId}번 문제 복습 완료! (${confidence} 선택)`);
        navigate('/mypage/review'); // 목록으로 돌아가기
    };

 return (
        <div className="app-container">
            <FixedFrame>
                {/* ❗ Header 대신 간단한 제목과 뒤로가기 버튼을 넣습니다. */}
                <header className="simple-header">
                    <button onClick={() => navigate(-1)} className="back-button">←</button>
                    <h1>문제 다시 풀기</h1>
                </header>
                <main className="review-detail-content">
                    <div className="problem-image-container">
                        <img src={mockProblemDetail.imageUrl} alt={`문제 ${problemId}`} />
                    </div>
                    <div className="review-controls">
                        <button 
                            className="answer-toggle-btn"
                            onClick={() => setAnswerVisible(!isAnswerVisible)}
                        >
                            {isAnswerVisible ? '정답 숨기기' : '정답 확인하기'}
                        </button>

                        {isAnswerVisible && (
                            <div className="answer-box">
                                <p>{mockProblemDetail.correctAnswer}</p>
                            </div>
                        )}

                        <div className="confidence-check">
                            <h4>자기 확신 체크</h4>
                            <div className="radio-group">
                                <label className={confidence === '확실함' ? 'checked' : ''}>
                                    <input type="radio" name="confidence" value="확실함" onChange={(e) => setConfidence(e.target.value)} />
                                    확실함
                                </label>
                                <label className={confidence === '모름' ? 'checked' : ''}>
                                    <input type="radio" name="confidence" value="모름" onChange={(e) => setConfidence(e.target.value)} />
                                    모름
                                </label>
                            </div>
                        </div>
                        
                        <button className="next-problem-btn" onClick={handleNextProblem}>
                            다음 문제로
                        </button>
                    </div>
                </main>
                <BottomNav />
            </FixedFrame>
        </div>
    );
};

export default ReviewDetailPage;