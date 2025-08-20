import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import './ChatHistorySidebar.css';

const ChatHistorySidebar = ({ isOpen, onClose }) => {
    const [chatList, setChatList] = useState([]);
    const { user, token, loadChatHistory, startNewChat } = useAuth();

    useEffect(() => {
        if (isOpen && user && token) {
            const fetchChatList = async () => {
                try {
                    const response = await fetch(`http://localhost:8080/api/problems?userId=${user.id}`, {
                        headers: { 'Authorization': `Bearer ${token}` }
                    });
                    if (!response.ok) throw new Error('API Error');
                    const data = await response.json();
                    setChatList(data);
                } catch (error) {
                    console.error("대화 목록 로딩 실패:", error);
                    alert('대화 목록을 불러오는 데 실패했습니다.');
                }
            };
            fetchChatList();
        }
    }, [isOpen, user, token]);

    const handleChatClick = (problemId) => {
        if (problemId === 'new') {
            startNewChat(); 
        } else {
            loadChatHistory(problemId); // 기존 대화 불러오기
        }
        onClose(); // 사이드바 닫기
    };

    return (
        <aside className={`sidebar ${isOpen ? 'show' : ''}`}>
            <h2>대화 목록</h2>
            <button className="new-chat-btn" onClick={() => handleChatClick('new')}>+ 새 대화 시작</button>
            {chatList.length > 0 ? (
                <ul>
                    {chatList.map((chat) => (
                        <li key={chat.problemId} onClick={() => handleChatClick(chat.problemId)}>
                            <span className="chat-title">{chat.title}</span>
                            <span className="chat-date">{new Date(chat.createdAt).toLocaleDateString()}</span>
                        </li>
                    ))}
                </ul>
            ) : (
                <p className="no-history">대화 기록이 없습니다.</p>
            )}
        </aside>
    );
};

export default ChatHistorySidebar;