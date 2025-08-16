import React from 'react';
import './SimilarProblems.css';

const SimilarProblems = ({ problems }) => {
  const safe = Array.isArray(problems) ? problems : [];

  if (safe.length === 0) {
    return (
      <div className="similar-problems-container">
        <h4>🤖 유사 문제를 생성했어요.</h4>
        <p style={{opacity:0.8}}>표시할 유사 문제가 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="similar-problems-container">
      <h4>🤖 유사 문제를 생성했어요.</h4>
      {safe.map((p, idx) => {
        const solution = p.solution_steps ?? p.solutionSteps ?? p.solution ?? '';
        return (
          <div key={idx} className="problem-card">
            <p className="problem-question"><strong>문제 {idx + 1}:</strong> {p.question}</p>
            <details>
              <summary>정답 및 풀이 보기</summary>
              <div className="solution-content">
                <p><strong>정답:</strong> {p.answer}</p>
                <p><strong>풀이:</strong> {solution}</p>
              </div>
            </details>
          </div>
        );
      })}
    </div>
  );
};

export default SimilarProblems;
