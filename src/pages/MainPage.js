// src/pages/MainPage.jsx
import React, { useState, useEffect } from 'react';
import Header from '../components/Layout/Header';
import BottomNav from '../components/Layout/BottomNav';
import QuestionInput from '../components/QuestionInput';
import AuthModal from '../components/AuthModal/AuthModal';
import ChatHistorySidebar from '../components/Layout/ChatHistorySidebar';
import FixedFrame from '../components/Layout/FixedFrame';
import { useAuth } from '../context/AuthContext';
import SimilarProblems from '../components/SimilarProblems';
import './MainPage.css';

const MainPage = () => {
  const [modalType, setModalType] = useState(null);
  const [isSidebarOpen, setSidebarOpen] = useState(false);

  const { messages, problemId, handleSendMessage, handleRequestSimilarProblems } = useAuth();

  useEffect(() => {
    const chatArea = document.getElementById('chat-messages');
    if (chatArea) chatArea.scrollTop = chatArea.scrollHeight;
  }, [messages]);

  const getInitial = (sender) => (sender === 'user' ? '나' : 'AI');

  return (
    <div className="app-container">
      {isSidebarOpen && <div className="backdrop" onClick={() => setSidebarOpen(false)}></div>}
      <ChatHistorySidebar isOpen={isSidebarOpen} onClose={() => setSidebarOpen(false)} />

      <FixedFrame>
        <Header
          onLoginClick={() => setModalType('login')}
          onMenuClick={() => setSidebarOpen(!isSidebarOpen)}
        />

        <main id="chat-messages" className="chat-messages-area">
          {messages.map((msg) => {
            const isUser = msg.sender === 'user';
            return (
              <div key={msg.id} className={`message-row ${isUser ? 'outgoing' : 'incoming'}`}>
                {/* 아바타 */}
                {!isUser && <div className="avatar avatar-ai" aria-hidden>{getInitial(msg.sender)}</div>}

                {/* 말풍선 */}
                <div className={`bubble ${isUser ? 'bubble-user' : 'bubble-ai'}`}>
                  {/* 이미지 (원본 문제/첨부) */}
                  {msg.file && <img src={msg.file} alt="첨부 이미지" className="bubble-image" />}

                  {/* ▼▼ 메시지 타입별 분기 ▼▼ */}
                  {msg.type === 'similar' && Array.isArray(msg.problems) ? (
                    <SimilarProblems problems={msg.problems} />
                  ) : (
                    // text가 있을 때만 출력 (빈말풍선 방지)
                    typeof msg.text === 'string' && msg.text.trim().length > 0 && (
                      msg.text.trim().startsWith('{')
                        ? <pre className="bubble-text code">{msg.text}</pre>
                        : <p className="bubble-text">{msg.text}</p>
                    )
                  )}

                  {/* 액션 버튼 (첫 분석 직후만) */}
                  {msg.isActionable && (
                    <div className="action-buttons">
                      <button onClick={handleRequestSimilarProblems}>유사 문제</button>
                    </div>
                  )}
                </div>

                {isUser && <div className="avatar avatar-user" aria-hidden>{getInitial(msg.sender)}</div>}
              </div>
            );
          })}
        </main>

        <QuestionInput onSendMessage={handleSendMessage} isFollowUp={!!problemId} />
        <BottomNav />
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
