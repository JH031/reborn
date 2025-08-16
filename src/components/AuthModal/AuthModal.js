// src/components/AuthModal/AuthModal.js

import React from 'react';
import './AuthModal.css';
import LoginForm from './LoginForm';
import SignupForm from './SignupForm';

const AuthModal = ({ type, onClose, switchType }) => {
  return (
    <>
      <div className="modal-overlay" onClick={onClose} />
      <div className="auth-modal">
        <button className="close-btn" onClick={onClose}>
          &#10005;
        </button>
        {/* LoginForm에 onClose prop을 전달합니다. */}
        {type === 'login' ? (
          <LoginForm switchType={switchType} onClose={onClose} />
        ) : (
          <SignupForm />
        )}
      </div>
    </>
  );
};

export default AuthModal;