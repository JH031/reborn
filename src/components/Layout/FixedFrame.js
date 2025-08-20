import React from 'react';
import './FixedFrame.css';

const FixedFrame = ({ children }) => {
  return (
    <div className="fixed-frame">
      {children}
    </div>
  );
};

export default FixedFrame;