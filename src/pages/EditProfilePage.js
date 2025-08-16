import React, { useState, useEffect} from 'react';
import { useNavigate } from 'react-router-dom';
import FixedFrame from '../components/Layout/FixedFrame';
import BottomNav from '../components/Layout/BottomNav';
import { useAuth } from '../context/AuthContext';
import axios from 'axios';
import './EditProfilePage.css';


const EditProfilePage = () => {
    const navigate = useNavigate();
    const { user, token, login } = useAuth(); // login 함수도 가져와서 정보 업데이트에 사용

    // 현재 로그인된 사용자 정보로 초기 상태 설정
    const [email, setEmail] = useState('');
    const [grade, setGrade] = useState('');
    const [school, setSchool] = useState('');

    useEffect(() => {
        if (user) {
            setEmail(user.email || '');
             setGrade(user.grade || ''); 
             setSchool(user.school || '');
        }
    }, [user]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!user) return;

        try {
            const response = await axios.patch(
                `http://localhost:8080/api/users/${user.id}/profile`,
                { email, grade: parseInt(grade, 10), school },
                { headers: { 'Authorization': `Bearer ${token}` } }
            );

            // ❗ 중요: 정보가 성공적으로 업데이트되면, AuthContext의 상태도 업데이트해야 합니다.
            // 백엔드가 업데이트된 사용자 정보를 응답으로 보내준다고 가정합니다.
            const updatedUser = { ...user, ...response.data };
            login({ token, ...updatedUser }); // login 함수를 재활용하여 상태 및 localStorage 업데이트

            alert('회원정보가 성공적으로 수정되었습니다.');
            navigate('/mypage'); // 마이페이지로 복귀
        } catch (error) {
            console.error("회원정보 수정 실패:", error);
            alert('정보 수정에 실패했습니다. 다시 시도해주세요.');
        }
    };

    return (
        <div className="app-container">
            <FixedFrame>
                <header className="simple-header">
                    <button onClick={() => navigate(-1)} className="back-button">←</button>
                    <h1>회원정보 수정</h1>
                </header>
                <main className="edit-profile-content">
                    <form onSubmit={handleSubmit} className="edit-profile-form">
                        <div className="form-group">
                            <label htmlFor="email">이메일</label>
                            <input
                                id="email"
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label htmlFor="grade">학년</label>
                            <input
                                id="grade"
                                type="number"
                                min="1"
                                max="6"
                                value={grade}
                                onChange={(e) => setGrade(e.target.value)}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label htmlFor="school">학교급</label>
                            <select id="school" value={school} onChange={(e) => setSchool(e.target.value)} required>
                                <option value="">선택하세요</option>
                                <option value="ELEMENTARY">초등학교</option>
                                <option value="MIDDLE">중학교</option>
                                <option value="HIGH">고등학교</option>
                            </select>
                        </div>
                        <button type="submit" className="save-button">저장하기</button>
                    </form>
                </main>
                <BottomNav />
            </FixedFrame>
        </div>
    );
};

export default EditProfilePage;