import React from 'react';
import './AuthModal.css';
import LoginForm from './LoginForm';
import SignupForm from './SignupForm';
import FindIdForm from './FindIdForm'; 

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
        {type === 'findId' && <FindIdForm switchType={switchType} />} 
      </div>
    </>
  );
};

export default AuthModal;