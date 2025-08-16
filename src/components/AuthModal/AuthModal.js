// src/components/AuthModal/AuthModal.js

import React from 'react';
import './AuthModal.css';
import LoginForm from './LoginForm';
import SignupForm from './SignupForm';
import FindIdForm from './FindIdForm'; // ★ 추가


const AuthModal = ({ type, onClose, switchType }) => {
  return (
    <>
      <div className="modal-overlay" onClick={onClose} />
      <div className="auth-modal">
        <button className="close-btn" onClick={onClose}>
          &#10005;
        </button>
        {type === 'login' && <LoginForm switchType={switchType} onClose={onClose} />}
        {type === 'signup' && <SignupForm />}
        {type === 'findId' && <FindIdForm switchType={switchType} />} {/* ★ 추가 */}
      </div>
    </>
  );
};

export default AuthModal;