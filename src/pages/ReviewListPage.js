import React from 'react';
import { Link, useNavigate } from 'react-router-dom'; // ❗ useNavigate import
import Header from '../components/Layout/Header';
import BottomNav from '../components/Layout/BottomNav';
import FixedFrame from '../components/Layout/FixedFrame';
import './ReviewListPage.css';

// API가 없으므로, 임시로 사용할 가짜 데이터
const mockReviewProblems = [
    {
        problemId: 1,
        subject: '수학',
        concept: '사칙연산 혼합 계산',
        imageUrl: 'https://via.placeholder.com/400x200.png?text=Problem+Image+1',
        reviewTurn: 3,
        daysAgo: 7,
    },
    {
        problemId: 2,
        subject: '코딩',
        concept: '재귀 함수',
        imageUrl: 'https://via.placeholder.com/400x200.png?text=Problem+Image+2',
        reviewTurn: 1,
        daysAgo: 1,
    },
];

const ReviewListPage = () => {
    const navigate = useNavigate(); // ❗ navigate 함수 생성

    return (
        <div className="app-container">
            <FixedFrame>
                <Header />
                <main className="review-list-content">
                    <div className="review-list-header">
                        {/* ❗ 뒤로 가기 버튼 추가 */}
                        <button onClick={() => navigate(-1)} className="back-button">←</button>
                        <h2>오늘의 복습</h2>
                    </div>
                    <p className="review-list-description">복습 주기에 맞춰 도착한 문제들이에요.</p>
                    <div className="review-card-list">
                        {mockReviewProblems.map(problem => (
                            <Link to={`/review/${problem.problemId}`} key={problem.problemId} className="review-card">
                                <div className="card-info">
                                    <span className="card-subject">{problem.subject}</span>
                                    <h3 className="card-concept">{problem.concept}</h3>
                                    <p className="card-meta">{problem.reviewTurn}회차 복습 ({problem.daysAgo}일 전)</p>
                                </div>
                                <img src={problem.imageUrl} alt={problem.concept} className="card-thumbnail" />
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
