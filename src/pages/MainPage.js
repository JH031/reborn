import React, { useState, useEffect } from 'react';
import Header from '../components/Layout/Header';
import BottomNav from '../components/Layout/BottomNav';
import QuestionInput from '../components/QuestionInput';
import AuthModal from '../components/AuthModal/AuthModal';
import ChatHistorySidebar from '../components/Layout/ChatHistorySidebar';
import FixedFrame from '../components/Layout/FixedFrame';
import { useAuth } from '../context/AuthContext';
import SimilarProblems from '../components/SimilarProblems';
import { useLocation } from 'react-router-dom';
import './MainPage.css';

const MainPage = () => {
  const [modalType, setModalType] = useState(null);
  const [isSidebarOpen, setSidebarOpen] = useState(false);
  const { messages, problemId, handleSendMessage, handleRequestSimilarProblems } = useAuth();
  const location = useLocation();


  useEffect(() => {
    const chatArea = document.getElementById('chat-messages');
    if (chatArea) chatArea.scrollTop = chatArea.scrollHeight;
  }, [messages]);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    if (params.get('auth') === 'login') {
      setModalType('login');
    }
  }, [location.search]);

  const getInitial = (sender) => (sender === 'user' ? '나' : 'AI');

  return (
    <div className="app-container">
      <FixedFrame>
        <div className="frame-layer">
          {isSidebarOpen && (
            <div
              className="frame-backdrop"
              onClick={() => setSidebarOpen(false)}
              aria-hidden
            />
          )}
          <ChatHistorySidebar
            isOpen={isSidebarOpen}
            onClose={() => setSidebarOpen(false)}
          />
        <Header
          onLoginClick={() => setModalType('login')}
          onMenuClick={() => setSidebarOpen(!isSidebarOpen)}
        />

        {/* 채팅 영역 */}
        <main id="chat-messages" className="chat-messages-area">
          {messages.map((msg) => {
            const isUser = msg.sender === 'user';
            return (
              <div
                key={msg.id}
                className={`message-row ${isUser ? 'outgoing' : 'incoming'}`}
              >
                {!isUser && (
                  <div className="avatar avatar-ai" aria-hidden>
                    {getInitial(msg.sender)}
                  </div>
                )}

                {/* 말풍선 */}
                <div className={`bubble ${isUser ? 'bubble-user' : 'bubble-ai'}`}>
                  {msg.file && (
                    <img src={msg.file} alt="첨부 이미지" className="bubble-image" />
                  )}

                 {msg.type === 'similar' ? (
                   <SimilarProblems problems={msg.problems || []} />
                 ) : (
                   typeof msg.text === 'string' &&
                   msg.text.length > 0 &&
                   (msg.text.trim().startsWith('{') ? (
                     <pre className="bubble-text code">{msg.text}</pre>
                 ) : (
                   <p className="bubble-text">{msg.text}</p>
                  ))
                )}

                  {msg.isActionable && (
                    <div className="action-buttons">
                      <button onClick={handleRequestSimilarProblems}>유사 문제</button>
                    </div>
                  )}
                </div>

                {isUser && (
                  <div className="avatar avatar-user" aria-hidden>
                    {getInitial(msg.sender)}
                  </div>
                )}
              </div>
            );
          })}
        </main>

        <div className="qi-dock">
          <QuestionInput onSendMessage={handleSendMessage} isFollowUp={!!problemId} />
        </div>
        <BottomNav />
        </div>
      </FixedFrame>

      {modalType && (
        <AuthModal
          type={modalType}
          onClose={() => setModalType(null)}
          switchType={(type) => setModalType(type)}
        />
      )}
    </div>
  );
};

export default MainPage;
