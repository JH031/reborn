import React, { useState } from 'react'; // ❗ 1. { useState } 추가
import './QuestionInput.css';

// ❗ 이제 QuestionInput은 onSendMessage와 isFollowUp prop을 다시 받습니다.
const QuestionInput = ({ onSendMessage, isFollowUp }) => {
    // 공통 상태
    const [file, setFile] = useState(null);
    const [previewUrl, setPreviewUrl] = useState('');
    // 추가 질문용 상태
    const [prompt, setPrompt] = useState('');

     const handleFileChange = (e) => {
        const selectedFile = e.target.files[0];
        if (selectedFile) {
            setFile(selectedFile);
            setPreviewUrl(URL.createObjectURL(selectedFile));
        }
    };

    const removeImage = () => {
        setFile(null);
        setPreviewUrl('');
        const fileInput = document.getElementById('file-input');
        if (fileInput) fileInput.value = '';
    };

    // 1. 첫 질문 (옵션 버튼) 전송 핸들러
    const handleInitialSend = (option) => {
        if (!file) {
            alert('먼저 문제 사진을 첨부해주세요.');
            return;
        }
        // 첫 질문 시에는 사용자 텍스트가 없으므로 prompt는 '' (빈 문자열)로 전달
        onSendMessage('', file, option);
        removeImage();
    };

    // 2. 추가 질문 전송 핸들러
    const handleFollowUpSend = (e) => {
        e.preventDefault();
        if (!prompt && !file) return;
        onSendMessage(prompt, file, null); // 추가 질문 시 option은 null
        setPrompt('');
        removeImage();
    };

    return (
        <div className="question-input-area">
            {previewUrl && (
                <div className="image-preview-wrapper">
                    <img src={previewUrl} alt="미리보기" />
                    <button onClick={removeImage} className="remove-image-btn">×</button>
                </div>
            )}

            {isFollowUp ? (
                // --- 2. 추가 질문용 입력창 ---
                <form className="follow-up-form" onSubmit={handleFollowUpSend}>
                    <label htmlFor="file-input" className="file-label">📎</label>
                    <input id="file-input" type="file" accept="image/*" onChange={handleFileChange} />
                    <input
                        type="text"
                        value={prompt}
                        onChange={(e) => setPrompt(e.target.value)}
                        placeholder="추가 질문을 입력하세요..."
                        className="text-input"
                    />
                    <button type="submit" className="send-button">전송</button>
                </form>
            ) : (
                // --- 1. 첫 질문용 입력창 ---
                <div className="initial-prompt-area">
                    <label htmlFor="file-input" className="file-label-large">
                        {file ? '사진이 첨부되었습니다. 아래 옵션을 선택하세요.' : '📎 사진을 첨부하여 질문을 시작하세요'}
                    </label>
                    <input id="file-input" type="file" accept="image/*" onChange={handleFileChange} />
                    
                    {file && (
                        <div className="option-buttons">
                            <button onClick={() => handleInitialSend('FIND_MY_ERROR')}>내 오류 찾기</button>
                            <button onClick={() => handleInitialSend('APPROACH')}>접근 방식</button>
                            <button onClick={() => handleInitialSend('FULL_SOLUTION')}>전체 풀이</button>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default QuestionInput;