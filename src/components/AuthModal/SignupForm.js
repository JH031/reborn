import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

const SignupForm = () => {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    name: '',
    id: '',
    password: '',
    confirmPassword: '',
    school: '',
    grade: '',
    email: '',
    // ✅ 백엔드 키와 동일한 이름으로 관리
    receiveReminders: false,
  });

  const [idCheck, setIdCheck] = useState({ checked: false, message: '' });
  const [gradeOptions, setGradeOptions] = useState([]);

  useEffect(() => {
    if (form.school === '중등' || form.school === '고등') {
      setGradeOptions(['1학년', '2학년', '3학년']);
    } else if (form.school === '초등') {
      setGradeOptions(['1학년', '2학년', '3학년', '4학년', '5학년', '6학년']);
    } else {
      setGradeOptions([]);
    }
    setForm(prev => ({ ...prev, grade: '' }));
  }, [form.school]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm(prev => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
    if (name === 'id') setIdCheck({ checked: false, message: '' });
  };

  const handleIdCheck = async () => {
    if (!form.id) {
      alert('아이디를 먼저 입력해주세요.');
      return;
    }
    try {
      await axios.get(`http://localhost:8080/api/users/check-id?userid=${form.id}`);
      setIdCheck({ checked: false, message: '이미 사용 중인 아이디입니다.' });
    } catch (error) {
      if (error.response && error.response.status === 404) {
        setIdCheck({ checked: true, message: '사용 가능한 아이디입니다.' });
      } else {
        console.error('아이디 중복 확인 중 오류:', error);
        setIdCheck({ checked: false, message: '오류가 발생했습니다. 다시 시도해주세요.' });
      }
    }
  };

  const handleSubmit = async () => {
    if (!idCheck.checked) {
      alert('아이디 중복 확인을 해주세요.');
      return;
    }
    if (!form.name || !form.id || !form.password || !form.school || !form.grade || !form.email) {
      alert('모든 필수 항목을 입력해주세요.');
      return;
    }
    if (form.password !== form.confirmPassword) {
      alert('비밀번호가 일치하지 않습니다.');
      return;
    }

    const schoolMapping = { '초등': 'ELEMENTARY', '중등': 'MIDDLE', '고등': 'HIGH' };

    // ✅ Swagger 스키마에 맞춘 페이로드
    const apiData = {
      userid: form.id,
      name: form.name,
      password: form.password,
      email: form.email,
      grade: parseInt(form.grade.replace('학년', ''), 10),
      school: schoolMapping[form.school],
      receiveReminders: !!form.receiveReminders,
    };

    try {
      await axios.post('http://localhost:8080/api/users/signup', apiData);
      alert('회원가입이 성공적으로 완료되었습니다!');
      navigate('/', { state: { openLoginModal: true } });
    } catch (error) {
      if (error.response) {
        alert(`회원가입 실패: ${error.response.data.message || '서버에서 오류가 발생했습니다.'}`);
      } else {
        alert('회원가입 중 알 수 없는 오류가 발생했습니다.');
      }
    }
  };

  return (
    <div>
      <h2>회원가입</h2>

      <input
        name="name"
        placeholder="이름"
        onChange={handleChange}
        value={form.name}
      />

      {/* 아이디 입력칸 내부 버튼 */}
      <div className="input-with-button">
        <input
          name="id"
          placeholder="아이디"
          onChange={handleChange}
          value={form.id}
          aria-describedby="id-check-msg"
        />
        <button
          type="button"
          className={`id-check-btn ${idCheck.checked ? 'ok' : ''}`}
          onClick={handleIdCheck}
        >
          중복 확인
        </button>
      </div>
      {idCheck.message && (
        <p id="id-check-msg" className={`check-message ${idCheck.checked ? 'success' : 'error'}`}>
          {idCheck.message}
        </p>
      )}

      <input
        name="password"
        type="password"
        placeholder="비밀번호"
        onChange={handleChange}
        value={form.password}
      />
      <input
        name="confirmPassword"
        type="password"
        placeholder="비밀번호 확인"
        onChange={handleChange}
        value={form.confirmPassword}
      />

      <div className="row">
        <select name="school" onChange={handleChange} value={form.school}>
          <option value="">학교</option>
          <option value="초등">초등</option>
          <option value="중등">중등</option>
          <option value="고등">고등</option>
        </select>
        <select name="grade" onChange={handleChange} value={form.grade}>
          <option value="">학년</option>
          {gradeOptions.map((g, i) => (
            <option key={i} value={g}>{g}</option>
          ))}
        </select>
      </div>

      <input
        name="email"
        placeholder="이메일"
        onChange={handleChange}
        value={form.email}
      />

      {/* ✅ 백엔드로 실제 값이 연결되는 체크박스 */}
      <label className="checkbox-row" htmlFor="receiveReminders">
        <input
          id="receiveReminders"
          type="checkbox"
          name="receiveReminders"
          onChange={handleChange}
          checked={form.receiveReminders}
        />
        <span>복습 이메일 수신에 동의합니다.</span>
      </label>

      <button className="submit-btn" onClick={handleSubmit}>가입하기</button>
    </div>
  );
};

export default SignupForm;
