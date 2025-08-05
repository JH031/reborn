import React, { useState } from 'react';
import FixedFrame from '../components/Layout/FixedFrame';
import Header from '../components/Layout/Header';
import BottomNav from '../components/Layout/BottomNav';
import QuestionInput from '../components/QuestionInput';
import AuthModal from '../components/AuthModal/AuthModal';

const MainPage = () => {
  const [modalType, setModalType] = useState(null); // 'login' or 'signup'
  

  return (
    <FixedFrame>
      <Header onLoginClick={() => setModalType('login')} onSignupClick={() => setModalType('signup')} />
      <QuestionInput />
      <BottomNav />
      {modalType && <AuthModal type={modalType} onClose={() => setModalType(null)} switchType={(type) => setModalType(type)} // ✅ 이걸 꼭 넣어야 함
/>}
    </FixedFrame>
  );
};

export default MainPage;
