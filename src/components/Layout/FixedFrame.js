// src/components/Layout/FixedFrame.js

import React from 'react';
import './FixedFrame.css';

// isOpen prop과 조건부 클래스를 모두 제거합니다.
const FixedFrame = ({ children }) => {
  return (
    <div className="fixed-frame">
      {children}
    </div>
  );
};

export default FixedFrame;