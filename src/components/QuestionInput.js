// src/components/QuestionInput.js
import React, { useState } from 'react';
import './QuestionInput.css';

const QuestionInput = () => {
  const [question, setQuestion] = useState('');

  const handleSend = () => {
    alert(`전송된 질문: ${question}`);
    setQuestion('');
  };

  return (
    <div className="question-box">
      <input
        type="text"
        placeholder="무엇이든 질문해보세요!"
        value={question}
        onChange={(e) => setQuestion(e.target.value)}
      />
      <button onClick={handleSend}>전송</button>
    </div>
  );
};

export default QuestionInput;
